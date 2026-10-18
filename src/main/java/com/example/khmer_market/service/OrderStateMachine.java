package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.Inventory;
import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.entity.OrderItem;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.exception.InvalidOrderTransitionException;
import com.example.khmer_market.repository.InventoryRepository;
import com.example.khmer_market.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStateMachine {

    private static final Logger log = LoggerFactory.getLogger(OrderStateMachine.class);

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final WalletService walletService;
    private final EscrowService escrowService;

    @org.springframework.beans.factory.annotation.Autowired
    public OrderStateMachine(OrderRepository orderRepository,
                             InventoryRepository inventoryRepository,
                             WalletService walletService,
                             @org.springframework.beans.factory.annotation.Autowired(required = false) EscrowService escrowService) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.walletService = walletService;
        this.escrowService = escrowService;
    }

    public OrderStateMachine(OrderRepository orderRepository,
                             InventoryRepository inventoryRepository) {
        this(orderRepository, inventoryRepository, null, null);
    }

    /**
     * Transition order to CONFIRMED.
     * Allowed actors: FARMER (for COD/Bank transfer) or SYSTEM (after verified online payment).
     */
    @Transactional
    public void confirmOrder(Order order, UserRole actorRole) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderTransitionException(
                    "Cannot confirm order in state '" + order.getStatus() + "'. Only 'pending' orders can be confirmed.");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Order {} transitioned from PENDING -> CONFIRMED by {}", order.getOrderNumber(), actorRole);
    }

    /**
     * Transition order to SHIPPED.
     * Allowed actor: FARMER.
     * Business rule: Deducts stock atomically in ONE update: quantity -= qty AND reserved_quantity -= qty.
     */
    @Transactional
    public void shipOrder(Order order, UserRole actorRole) {
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderTransitionException(
                    "Cannot ship order in state '" + order.getStatus() + "'. Only 'confirmed' orders can be shipped.");
        }

        if (actorRole != UserRole.FARMER && actorRole != UserRole.ADMIN) {
            throw new InvalidOrderTransitionException("Only the seller or administrator can ship orders.");
        }

        // Deduct inventory: physical quantity and reserved quantity decrement together
        for (OrderItem item : order.getItems()) {
            Inventory inv = inventoryRepository.findByPackageIdForUpdate(item.getProductPackage().getId())
                    .orElseThrow(() -> new IllegalStateException("Missing inventory record for package ID: " + item.getProductPackage().getId()));

            int qty = item.getQuantity();
            int newQuantity = inv.getQuantity() - qty;
            int newReserved = inv.getReservedQuantity() - qty;

            if (newQuantity < 0 || newReserved < 0) {
                throw new IllegalStateException("Inventory underflow detected for package ID: " + item.getProductPackage().getId());
            }

            inv.setQuantity(newQuantity);
            inv.setReservedQuantity(newReserved);
            inventoryRepository.save(inv);
        }

        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);
        log.info("Order {} transitioned from CONFIRMED -> SHIPPED by {}. Stock deducted.", order.getOrderNumber(), actorRole);
    }

    /**
     * Transition order to DELIVERED.
     * Allowed actors: FARMER (delivery confirmation) or BUYER (receipt confirmation).
     */
    @Transactional
    public void deliverOrder(Order order, UserRole actorRole) {
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOrderTransitionException(
                    "Cannot deliver order in state '" + order.getStatus() + "'. Only 'shipped' orders can be marked delivered.");
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
        log.info("Order {} transitioned from SHIPPED -> DELIVERED by {}", order.getOrderNumber(), actorRole);

        // Release escrow upon verified delivery
        if (escrowService != null && order.isUnderEscrowHold()) {
            escrowService.releaseEscrow(order, "Order Delivered & Confirmed by " + actorRole);
        }
    }

    /**
     * Cancel order and release reservations or stock.
     * Allowed actors:
     * - BUYER: only while PENDING (and cannot cancel if under 7-day escrow hold)
     * - FARMER: while PENDING or CONFIRMED
     * - ADMIN: any time
     */
    @Transactional
    public void cancelOrder(Order order, UserRole actorRole, String reason) {
        OrderStatus current = order.getStatus();

        if (current == OrderStatus.CANCELLED) {
            return; // Idempotent
        }

        // 7-day Escrow anti-refund validation: Buyers cannot refund or cancel while order is under 7-day escrow hold
        if (actorRole == UserRole.BUYER && order.isUnderEscrowHold()) {
            throw new InvalidOrderTransitionException("This order is protected under the 7-day merchant fulfillment escrow window. Refunds and cancellations are locked during this protection period.");
        }

        if (actorRole == UserRole.BUYER && current != OrderStatus.PENDING) {
            throw new InvalidOrderTransitionException("Buyers can only cancel orders while in 'pending' status.");
        }

        if (actorRole == UserRole.FARMER && current != OrderStatus.PENDING && current != OrderStatus.CONFIRMED) {
            throw new InvalidOrderTransitionException("Farmers can only cancel orders while 'pending' or 'confirmed'.");
        }

        // Release inventory reservation if cancelled before shipping
        if (current == OrderStatus.PENDING || current == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                Inventory inv = inventoryRepository.findByPackageIdForUpdate(item.getProductPackage().getId())
                        .orElse(null);
                if (inv != null) {
                    int newReserved = Math.max(0, inv.getReservedQuantity() - item.getQuantity());
                    inv.setReservedQuantity(newReserved);
                    inventoryRepository.save(inv);
                }
            }
            log.info("Released inventory reservation for cancelled order {}", order.getOrderNumber());
        } else if (current == OrderStatus.SHIPPED || current == OrderStatus.DELIVERED) {
            // Admin cancellation after shipping: restore physical quantity
            if (actorRole == UserRole.ADMIN) {
                for (OrderItem item : order.getItems()) {
                    Inventory inv = inventoryRepository.findByPackageIdForUpdate(item.getProductPackage().getId())
                            .orElse(null);
                    if (inv != null) {
                        inv.setQuantity(inv.getQuantity() + item.getQuantity());
                        inventoryRepository.save(inv);
                    }
                }
                log.info("Admin cancelled order {} after shipping. Restored physical inventory.", order.getOrderNumber());
            }
        }

        // Financial Refund: If order has paid payments, automatically refund buyer and claw back disbursements
        if (walletService != null) {
            walletService.refundOrder(order, reason);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.warn("Order {} CANCELLED by {} (Reason: {})", order.getOrderNumber(), actorRole, reason);
    }
}
