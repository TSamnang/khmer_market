package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.entity.Payment;
import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.domain.enums.PaymentStatus;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.repository.OrderRepository;
import com.example.khmer_market.repository.PaymentRepository;
import com.example.khmer_market.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EscrowService {

    private static final Logger log = LoggerFactory.getLogger(EscrowService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletService walletService;
    private final StoreConfigService storeConfigService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public EscrowService(OrderRepository orderRepository,
                         PaymentRepository paymentRepository,
                         WalletService walletService,
                         StoreConfigService storeConfigService,
                         UserRepository userRepository,
                         AuditService auditService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
        this.storeConfigService = storeConfigService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Places a paid order into the 7-day Platform Master Bank Escrow.
     * Seller balance is NOT credited yet. Buyer refund is locked during this period.
     */
    @Transactional
    public void holdOrderInEscrow(Order order, int holdDays) {
        if (order == null) return;

        int days = holdDays > 0 ? holdDays : 7;
        order.setEscrowStatus("HELD");
        order.setEscrowReleaseAt(LocalDateTime.now().plusDays(days));
        orderRepository.save(order);

        log.info("Order {} placed in 7-day Platform Bank Escrow. Auto-confirmation scheduled for {}",
                order.getOrderNumber(), order.getEscrowReleaseAt());
    }

    /**
     * Confirms and releases escrow funds to the seller's website balance.
     * Keeps platform commission/tax in the Master Bank account.
     */
    @Transactional
    public boolean releaseEscrow(Order order, String reason) {
        if (order == null || order.isEscrowReleased()) {
            return false; // Idempotent
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Cannot release escrow for cancelled order {}", order.getOrderNumber());
            return false;
        }

        // Verify order has a PAID payment record
        Payment payment = null;
        if (order.getPayments() != null) {
            payment = order.getPayments().stream()
                    .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
                    .findFirst()
                    .orElse(null);
        }

        User seller = order.getSeller();
        if (seller != null && payment != null) {
            // Disburse produce net earnings + seller shipping to seller's website balance
            // and platform commission/tax to platform admin
            walletService.distributeOrderEarnings(
                    seller.getId(),
                    order.getSubtotal(),
                    order.getShippingFee(),
                    order.getOrderNumber()
            );
        }

        order.setEscrowStatus("RELEASED");
        order.setEscrowReleasedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Escrow successfully RELEASED for Order {}. Seller {} credited. Platform retained tax in Master Bank. (Reason: {})",
                order.getOrderNumber(), (seller != null ? seller.getUsername() : "N/A"), reason);

        auditService.logAction(
                "SYSTEM_ESCROW",
                "ESCROW_RELEASE",
                "ORDER",
                order.getOrderNumber(),
                "127.0.0.1",
                Map.of(
                        "orderNumber", order.getOrderNumber(),
                        "totalAmount", order.getTotalAmount(),
                        "seller", (seller != null ? seller.getUsername() : "unknown"),
                        "reason", reason != null ? reason : "Auto-confirmed"
                )
        );

        return true;
    }

    /**
     * Auto-confirms any order whose 7-day escrow hold has expired.
     */
    @Transactional
    public int autoConfirmExpiredEscrows() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> readyOrders = orderRepository.findHeldEscrowsReadyForRelease(now);
        int count = 0;

        for (Order order : readyOrders) {
            try {
                if (releaseEscrow(order, "7-Day Escrow Period Expired - Auto-Confirmed")) {
                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to auto-confirm escrow for order {}: {}", order.getOrderNumber(), e.getMessage());
            }
        }

        if (count > 0) {
            log.info("7-Day Escrow Engine auto-confirmed and released payouts for {} orders.", count);
        }
        return count;
    }

    /**
     * Scheduled background task: checks every 60 seconds for expired 7-day escrows.
     */
    @Scheduled(fixedRate = 60000)
    public void runScheduledAutoConfirm() {
        autoConfirmExpiredEscrows();
    }

    /**
     * Retrieves recent escrow transactions for admin monitor table.
     */
    @Transactional(readOnly = true)
    public Page<Order> getEscrowOrders(Pageable pageable) {
        return orderRepository.findEscrowOrders(pageable);
    }

    /**
     * Calculates financial breakdown of Master Bank and Escrow holdings.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEscrowSummary() {
        Map<String, Object> summary = new HashMap<>();

        BigDecimal heldEscrow = orderRepository.sumHeldEscrowAmount();
        BigDecimal releasedEscrow = orderRepository.sumReleasedEscrowAmount();
        BigDecimal totalPaidRevenue = orderRepository.sumTotalPaidRevenue();

        BigDecimal adminBalance = userRepository.findFirstByRole(UserRole.ADMIN)
                .map(User::getBalance)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        summary.put("heldEscrowAmount", heldEscrow != null ? heldEscrow.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        summary.put("releasedEscrowAmount", releasedEscrow != null ? releasedEscrow.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalPaidRevenue", totalPaidRevenue != null ? totalPaidRevenue.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        summary.put("retainedPlatformTax", adminBalance);
        summary.put("escrowHoldDays", storeConfigService.getEscrowHoldDays());
        summary.put("bankName", storeConfigService.getBankName());
        summary.put("bankAccountNumber", storeConfigService.getBankAccountNumber());
        summary.put("bankAccountName", storeConfigService.getBankAccountName());
        summary.put("bankEnvironment", storeConfigService.getBankEnvironment());

        return summary;
    }
}
