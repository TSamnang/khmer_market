package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.domain.enums.PackageStatus;
import com.example.khmer_market.domain.enums.PaymentMethod;
import com.example.khmer_market.domain.enums.PaymentStatus;
import com.example.khmer_market.exception.OutOfStockException;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.payment.PaymentProvider;
import com.example.khmer_market.payment.PaymentProviderFactory;
import com.example.khmer_market.payment.PaymentResult;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.util.OrderNumberGenerator;
import com.example.khmer_market.web.dto.CheckoutDto;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private static final String CHECKOUT_TOKEN_SESSION_KEY = "CURRENT_CHECKOUT_TOKEN";

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final PaymentProviderFactory paymentProviderFactory;
    private final WalletService walletService;

    @Value("${market.business.shipping-flat-rate:1.50}")
    private BigDecimal shippingFlatRate = new BigDecimal("1.50");

    @Value("${market.business.shipping-free-threshold:25.00}")
    private BigDecimal shippingFreeThreshold = new BigDecimal("25.00");

    private com.example.khmer_market.service.StoreConfigService storeConfigService;
    private com.example.khmer_market.service.EscrowService escrowService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setStoreConfigService(com.example.khmer_market.service.StoreConfigService storeConfigService) {
        this.storeConfigService = storeConfigService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setEscrowService(com.example.khmer_market.service.EscrowService escrowService) {
        this.escrowService = escrowService;
    }

    public CheckoutService(UserRepository userRepository,
                           AddressRepository addressRepository,
                           CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           InventoryRepository inventoryRepository,
                           OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           PaymentRepository paymentRepository,
                           OrderNumberGenerator orderNumberGenerator,
                           PaymentProviderFactory paymentProviderFactory,
                           WalletService walletService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.orderNumberGenerator = orderNumberGenerator;
        this.paymentProviderFactory = paymentProviderFactory;
        this.walletService = walletService;
    }

    public String generateCheckoutToken(HttpSession session) {
        String token = UUID.randomUUID().toString();
        session.setAttribute(CHECKOUT_TOKEN_SESSION_KEY, token);
        return token;
    }

    public boolean validateAndConsumeToken(HttpSession session, String token) {
        if (token == null || session == null) {
            return false;
        }
        String sessionToken = (String) session.getAttribute(CHECKOUT_TOKEN_SESSION_KEY);
        if (sessionToken != null && sessionToken.equals(token)) {
            session.removeAttribute(CHECKOUT_TOKEN_SESSION_KEY);
            return true;
        }
        return false;
    }

    @Transactional
    public List<Order> processCheckout(Long buyerId, CheckoutDto dto, HttpSession session) {
        // Enforce single-use idempotency token to prevent double-submit
        if (!validateAndConsumeToken(session, dto.getCheckoutToken())) {
            throw new IllegalArgumentException("This checkout request has expired or was already processed. Please review your orders.");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found: " + buyerId));

        Cart cart = cartRepository.findByUserId(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Resolve or create delivery address
        Address address;
        if (dto.getAddressId() != null) {
            address = addressRepository.findByIdAndUserId(dto.getAddressId(), buyerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Selected delivery address not found"));
        } else if (dto.getNewAddress() != null && dto.getNewAddress().getName() != null) {
            address = new Address();
            address.setUser(buyer);
            address.setName(dto.getNewAddress().getName().trim());
            address.setPhone(dto.getNewAddress().getPhone().trim());
            address.setProvince(dto.getNewAddress().getProvince().trim());
            address.setAddress(dto.getNewAddress().getAddress() != null ? dto.getNewAddress().getAddress().trim() : "");
            address = addressRepository.save(address);
        } else {
            throw new IllegalArgumentException("A valid delivery address must be selected or provided");
        }

        PaymentMethod paymentMethod = PaymentMethod.fromValue(dto.getPaymentMethod());

        // Prevent sellers from purchasing their own products
        for (CartItem item : cartItems) {
            ProductPackage pkg = item.getProductPackage();
            if (pkg.getProduct().getSeller() != null && pkg.getProduct().getSeller().getId().equals(buyerId)) {
                throw new IllegalArgumentException("You cannot purchase your own product: " + pkg.getProduct().getName());
            }
        }

        // 1. Deadlock Prevention: Sort all items strictly by package_id ascending before acquiring locks
        List<CartItem> sortedItems = new ArrayList<>(cartItems);
        sortedItems.sort(Comparator.comparing(ci -> ci.getProductPackage().getId()));

        // 2. Lock each inventory row with PESSIMISTIC_WRITE and reserve quantity
        for (CartItem item : sortedItems) {
            ProductPackage pkg = item.getProductPackage();
            if (pkg.getStatus() != PackageStatus.ACTIVE || !pkg.getProduct().isActive() || !pkg.getProduct().getSeller().isActive()) {
                throw new IllegalArgumentException("Package '" + pkg.getPackageName() + "' is no longer available.");
            }

            Inventory inv = inventoryRepository.findByPackageIdForUpdate(pkg.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for package: " + pkg.getId()));

            int available = inv.getQuantity() - inv.getReservedQuantity();
            if (available < item.getQuantity()) {
                throw new OutOfStockException("Insufficient stock for '" + pkg.getPackageName() + "'. Available: " + available);
            }

            // Reserve stock
            inv.setReservedQuantity(inv.getReservedQuantity() + item.getQuantity());
            inventoryRepository.save(inv);
        }

        // 3. Multi-vendor Partitioning: Group items by Seller ID -> ONE ORDER PER SELLER
        Map<Long, List<CartItem>> itemsBySeller = new HashMap<>();
        for (CartItem item : cartItems) {
            Long sellerId = item.getProductPackage().getProduct().getSeller().getId();
            itemsBySeller.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(item);
        }

        BigDecimal effectiveThreshold = (storeConfigService != null) ? storeConfigService.getShippingFreeThreshold() : shippingFreeThreshold;
        BigDecimal effectiveFlatRate = (storeConfigService != null) ? storeConfigService.getShippingFlatRate() : shippingFlatRate;

        // Upfront validation: Check if buyer balance covers all partition orders
        if (paymentMethod == PaymentMethod.BALANCE) {
            BigDecimal grandTotal = BigDecimal.ZERO;
            for (List<CartItem> sellerItemsList : itemsBySeller.values()) {
                BigDecimal sellerSubtotal = BigDecimal.ZERO;
                for (CartItem ci : sellerItemsList) {
                    sellerSubtotal = sellerSubtotal.add(ci.getProductPackage().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
                }
                BigDecimal sellerShipping = (sellerSubtotal.compareTo(effectiveThreshold) >= 0)
                        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : effectiveFlatRate.setScale(2, RoundingMode.HALF_UP);
                grandTotal = grandTotal.add(sellerSubtotal.add(sellerShipping));
            }

            BigDecimal currentBalance = walletService.getBalance(buyerId);
            if (currentBalance.compareTo(grandTotal) < 0) {
                throw new IllegalArgumentException("Insufficient account balance. Available: $" + currentBalance + ", Order total: $" + grandTotal.setScale(2, RoundingMode.HALF_UP) + ". Please deposit funds or choose another payment method.");
            }
        }

        List<Order> createdOrders = new ArrayList<>();
        PaymentProvider provider = paymentProviderFactory.getProvider(paymentMethod);

        for (Map.Entry<Long, List<CartItem>> entry : itemsBySeller.entrySet()) {
            List<CartItem> sellerItems = entry.getValue();

            BigDecimal subtotal = BigDecimal.ZERO;
            for (CartItem item : sellerItems) {
                BigDecimal itemPrice = item.getProductPackage().getPrice();
                BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(lineTotal);
            }

            // Shipping fee calculation per seller order (dynamically configured)
            BigDecimal shippingFee = (subtotal.compareTo(effectiveThreshold) >= 0)
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : effectiveFlatRate.setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalAmount = subtotal.add(shippingFee).setScale(2, RoundingMode.HALF_UP);

            Order order = new Order();
            order.setBuyer(buyer);
            order.setAddress(address);
            order.setOrderNumber(orderNumberGenerator.generateUniqueOrderNumber());
            order.setSubtotal(subtotal);
            order.setShippingFee(shippingFee);
            order.setTotalAmount(totalAmount);
            order.setStatus(OrderStatus.PENDING);
            Order savedOrder = orderRepository.save(order);

            // Snapshot order items
            for (CartItem item : sellerItems) {
                ProductPackage pkg = item.getProductPackage();
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setProductPackage(pkg);
                orderItem.setSeller(pkg.getProduct().getSeller());
                orderItem.setPackageName(pkg.getPackageName());
                orderItem.setUnitPrice(pkg.getPrice());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setSubtotal(pkg.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP));
                orderItemRepository.save(orderItem);
                savedOrder.addItem(orderItem);
            }

            // Initialize payment record
            PaymentResult paymentResult = provider.initiatePayment(savedOrder);
            Payment payment = new Payment();
            payment.setOrder(savedOrder);
            payment.setPaymentMethod(paymentMethod);
            payment.setTransactionId(paymentResult.getTransactionId());
            payment.setAmount(totalAmount);

            if (paymentMethod == PaymentMethod.BALANCE) {
                // Deduct buyer balance
                walletService.payOrder(buyerId, totalAmount, savedOrder.getOrderNumber());

                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaymentDate(java.time.LocalDateTime.now());
                savedOrder.setStatus(OrderStatus.CONFIRMED);

                // Place in 7-day Platform Master Bank Escrow
                if (escrowService != null) {
                    int holdDays = (storeConfigService != null) ? storeConfigService.getEscrowHoldDays() : 7;
                    escrowService.holdOrderInEscrow(savedOrder, holdDays);
                } else {
                    walletService.distributeOrderEarnings(entry.getKey(), subtotal, shippingFee, savedOrder.getOrderNumber());
                }

                orderRepository.save(savedOrder);
            } else if (paymentMethod == PaymentMethod.CARD) {
                // Card payment captured into Platform Master Bank -> place in 7-day Escrow
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaymentDate(java.time.LocalDateTime.now());
                savedOrder.setStatus(OrderStatus.CONFIRMED);

                if (escrowService != null) {
                    int holdDays = (storeConfigService != null) ? storeConfigService.getEscrowHoldDays() : 7;
                    escrowService.holdOrderInEscrow(savedOrder, holdDays);
                } else {
                    walletService.distributeOrderEarnings(entry.getKey(), subtotal, shippingFee, savedOrder.getOrderNumber());
                }

                orderRepository.save(savedOrder);
            } else {
                payment.setPaymentStatus(PaymentStatus.PENDING);
            }

            paymentRepository.save(payment);
            savedOrder.addPayment(payment);

            createdOrders.add(savedOrder);
            log.info("Created order {} for buyer ID {} with seller ID {}", savedOrder.getOrderNumber(), buyerId, entry.getKey());
        }

        // 4. Clear the buyer's cart after successful order creation
        cartItemRepository.deleteAllByCartId(cart.getId());

        return createdOrders;
    }
}
