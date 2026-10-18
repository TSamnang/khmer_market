package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.entity.WalletTransaction;
import com.example.khmer_market.domain.enums.WalletTransactionStatus;
import com.example.khmer_market.domain.enums.WalletTransactionType;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.entity.Payment;
import com.example.khmer_market.domain.enums.PaymentStatus;
import com.example.khmer_market.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import com.example.khmer_market.domain.enums.UserRole;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final StoreConfigService storeConfigService;
    private final PaymentRepository paymentRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public WalletService(UserRepository userRepository,
                         WalletTransactionRepository walletTransactionRepository,
                         StoreConfigService storeConfigService,
                         PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.storeConfigService = storeConfigService;
        this.paymentRepository = paymentRepository;
    }

    public WalletService(UserRepository userRepository,
                         WalletTransactionRepository walletTransactionRepository,
                         StoreConfigService storeConfigService) {
        this(userRepository, walletTransactionRepository, storeConfigService, null);
    }

    public WalletService(UserRepository userRepository, WalletTransactionRepository walletTransactionRepository) {
        this(userRepository, walletTransactionRepository, null, null);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return userRepository.findById(userId)
                .map(User::getBalance)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Transactional
    public WalletTransaction deposit(Long userId, BigDecimal amount, String source, String reference) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be strictly greater than $0.00");
        }

        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        user.creditBalance(scaledAmount);
        userRepository.save(user);

        String ref = (reference != null && !reference.isBlank()) ? reference : "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String src = (source != null && !source.isBlank()) ? source : "Instant Deposit";

        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(WalletTransactionType.DEPOSIT);
        tx.setAmount(scaledAmount);
        tx.setBalanceAfter(user.getBalance());
        tx.setStatus(WalletTransactionStatus.COMPLETED);
        tx.setReference(ref);
        tx.setDescription("Deposit via " + src);

        WalletTransaction savedTx = walletTransactionRepository.save(tx);
        log.info("User {} deposited ${}. New balance: ${}", userId, scaledAmount, user.getBalance());
        return savedTx;
    }

    @Transactional
    public WalletTransaction withdraw(Long userId, BigDecimal amount, String channel, String accountNumber, String accountName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be strictly greater than $0.00");
        }
        if (amount.compareTo(new BigDecimal("1.00")) < 0) {
            throw new IllegalArgumentException("Minimum withdrawal amount is $1.00");
        }

        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getBalance().compareTo(scaledAmount) < 0) {
            throw new IllegalArgumentException("Insufficient balance ($" + user.getBalance() + " available, requested $" + scaledAmount + ")");
        }

        BigDecimal configuredFee = (user.getRole() == UserRole.ADMIN)
                ? BigDecimal.ZERO
                : ((storeConfigService != null) ? storeConfigService.getWithdrawalFee() : new BigDecimal("0.25"));
        final BigDecimal fee = (configuredFee != null && configuredFee.compareTo(BigDecimal.ZERO) >= 0)
                ? configuredFee.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal netPayout = scaledAmount.subtract(fee);
        if (netPayout.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than the processing fee ($" + fee + ")");
        }

        user.debitBalance(scaledAmount);
        userRepository.save(user);

        String ch = (channel != null && !channel.isBlank()) ? channel : "Bank Account";
        String accNum = (accountNumber != null && !accountNumber.isBlank()) ? accountNumber : "N/A";
        String accName = (accountName != null && !accountName.isBlank()) ? accountName : user.getUsername();

        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(WalletTransactionType.WITHDRAW);
        tx.setAmount(scaledAmount);
        tx.setBalanceAfter(user.getBalance());
        tx.setStatus(WalletTransactionStatus.COMPLETED);
        tx.setReference("WD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setDescription("Withdrawal to " + ch + " (" + accNum + " - " + accName + ") [Net: $" + netPayout + ", Fee: $" + fee + "]");

        WalletTransaction savedTx = walletTransactionRepository.save(tx);

        // Credit platform withdrawal fee to Platform Admin (only from non-admin users)
        if (fee.compareTo(BigDecimal.ZERO) > 0 && user.getRole() != UserRole.ADMIN) {
            userRepository.findFirstByRole(UserRole.ADMIN).ifPresent(admin -> {
                admin.creditBalance(fee);
                userRepository.save(admin);

                WalletTransaction feeTx = new WalletTransaction();
                feeTx.setUser(admin);
                feeTx.setType(WalletTransactionType.FEE);
                feeTx.setAmount(fee);
                feeTx.setBalanceAfter(admin.getBalance());
                feeTx.setStatus(WalletTransactionStatus.COMPLETED);
                feeTx.setReference(tx.getReference());
                feeTx.setDescription("Platform Withdrawal Fee: " + user.getUsername() + " via " + ch);
                walletTransactionRepository.save(feeTx);
                log.info("Platform Admin credited ${} withdrawal processing fee for transaction {}", fee, tx.getReference());
            });
        }

        log.info("User {} withdrew ${} via {}. Remaining balance: ${}", userId, scaledAmount, ch, user.getBalance());
        return savedTx;
    }

    @Transactional
    public WalletTransaction payOrder(Long userId, BigDecimal amount, String orderNumber) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than $0.00");
        }

        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getBalance().compareTo(scaledAmount) < 0) {
            throw new IllegalArgumentException("Insufficient balance ($" + user.getBalance() + " available, order requires $" + scaledAmount + "). Please deposit funds or choose another payment method.");
        }

        user.debitBalance(scaledAmount);
        userRepository.save(user);

        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(WalletTransactionType.PAYMENT);
        tx.setAmount(scaledAmount);
        tx.setBalanceAfter(user.getBalance());
        tx.setStatus(WalletTransactionStatus.COMPLETED);
        tx.setReference(orderNumber);
        tx.setDescription("Order Payment: " + orderNumber);

        WalletTransaction savedTx = walletTransactionRepository.save(tx);
        log.info("User {} paid ${} for Order {}. Remaining balance: ${}", userId, scaledAmount, orderNumber, user.getBalance());
        return savedTx;
    }

    @Transactional
    public WalletTransaction distributeOrderEarnings(Long sellerId, BigDecimal subtotal, BigDecimal shippingFee, String orderNumber) {
        if (sellerId == null) {
            return null;
        }
        BigDecimal safeSubtotal = (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0)
                ? subtotal.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal safeShipping = (shippingFee != null && shippingFee.compareTo(BigDecimal.ZERO) > 0)
                ? shippingFee.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal configuredCommRate = (storeConfigService != null) ? storeConfigService.getCommissionPercentage() : new BigDecimal("5.00");
        final BigDecimal commissionRate = (configuredCommRate != null && configuredCommRate.compareTo(BigDecimal.ZERO) >= 0)
                ? configuredCommRate
                : new BigDecimal("5.00");

        // Commission deducted ONLY from produce subtotal (shipping belongs 100% to seller)
        BigDecimal commissionAmount = safeSubtotal.multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // Seller payout = (subtotal - commission) + shippingFee
        BigDecimal sellerProducePayout = safeSubtotal.subtract(commissionAmount);
        BigDecimal sellerTotalPayout = sellerProducePayout.add(safeShipping).setScale(2, RoundingMode.HALF_UP);

        User seller = userRepository.findByIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerId));

        seller.creditBalance(sellerTotalPayout);
        userRepository.save(seller);

        WalletTransaction sellerTx = new WalletTransaction();
        sellerTx.setUser(seller);
        sellerTx.setType(WalletTransactionType.EARNING);
        sellerTx.setAmount(sellerTotalPayout);
        sellerTx.setBalanceAfter(seller.getBalance());
        sellerTx.setStatus(WalletTransactionStatus.COMPLETED);
        sellerTx.setReference(orderNumber);
        sellerTx.setDescription("Order Earnings: " + orderNumber + " (Produce: $" + safeSubtotal + " - Commission " + commissionRate + "% [-$" + commissionAmount + "] + Seller Shipping: $" + safeShipping + ")");
        WalletTransaction savedSellerTx = walletTransactionRepository.save(sellerTx);

        // Credit Platform Commission to Admin
        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            userRepository.findFirstByRole(UserRole.ADMIN).ifPresent(admin -> {
                admin.creditBalance(commissionAmount);
                userRepository.save(admin);

                WalletTransaction commTx = new WalletTransaction();
                commTx.setUser(admin);
                commTx.setType(WalletTransactionType.COMMISSION);
                commTx.setAmount(commissionAmount);
                commTx.setBalanceAfter(admin.getBalance());
                commTx.setStatus(WalletTransactionStatus.COMPLETED);
                commTx.setReference(orderNumber);
                commTx.setDescription("Platform Commission (" + commissionRate + "%): " + orderNumber + " (Seller: " + seller.getUsername() + ", Subtotal: $" + safeSubtotal + ")");
                walletTransactionRepository.save(commTx);
                log.info("Platform Admin credited ${} commission for Order {}", commissionAmount, orderNumber);
            });
        }

        log.info("Order {}: Seller {} credited ${} (Produce: ${}, Commission: -${}, Shipping: +${}). New balance: ${}",
                orderNumber, sellerId, sellerTotalPayout, safeSubtotal, commissionAmount, safeShipping, seller.getBalance());
        return savedSellerTx;
    }

    @Transactional
    public WalletTransaction creditOrderEarnings(Long sellerId, BigDecimal amount, String orderNumber) {
        return distributeOrderEarnings(sellerId, amount, BigDecimal.ZERO, orderNumber);
    }

    @Transactional
    public void refundOrder(Order order, String reason) {
        if (order == null) return;

        Payment payment = null;
        if (order.getPayments() != null) {
            payment = order.getPayments().stream()
                    .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
                    .findFirst()
                    .orElse(null);
        }

        // If no payment was completed, nothing to refund financially
        if (payment == null) {
            log.info("Order {} cancelled without PAID payment. No financial refund required.", order.getOrderNumber());
            return;
        }

        BigDecimal refundAmount = order.getTotalAmount();
        User buyer = order.getBuyer();
        User seller = order.getSeller();

        // 1. Credit 100% refund to Buyer Wallet
        if (buyer != null && refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            User buyerLocked = userRepository.findByIdForUpdate(buyer.getId()).orElse(buyer);
            buyerLocked.creditBalance(refundAmount);
            userRepository.save(buyerLocked);

            WalletTransaction buyerRefundTx = new WalletTransaction();
            buyerRefundTx.setUser(buyerLocked);
            buyerRefundTx.setType(WalletTransactionType.REFUND);
            buyerRefundTx.setAmount(refundAmount);
            buyerRefundTx.setBalanceAfter(buyerLocked.getBalance());
            buyerRefundTx.setStatus(WalletTransactionStatus.COMPLETED);
            buyerRefundTx.setReference(order.getOrderNumber());
            buyerRefundTx.setDescription("Refund for Order " + order.getOrderNumber() + ": " + (reason != null ? reason : "Order Cancelled"));
            walletTransactionRepository.save(buyerRefundTx);
            log.info("Refunded ${} to Buyer {} for cancelled Order {}", refundAmount, buyerLocked.getUsername(), order.getOrderNumber());
        }

        // 2. Calculate seller and platform clawback amounts based on original distribution formula
        // Note: Only claw back if earnings were actually released from escrow to the seller & admin
        boolean wasDisbursed = order.isEscrowReleased();

        if (wasDisbursed) {
            BigDecimal safeSubtotal = (order.getSubtotal() != null && order.getSubtotal().compareTo(BigDecimal.ZERO) > 0)
                    ? order.getSubtotal().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal safeShipping = (order.getShippingFee() != null && order.getShippingFee().compareTo(BigDecimal.ZERO) > 0)
                    ? order.getShippingFee().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal configuredCommRate = (storeConfigService != null) ? storeConfigService.getCommissionPercentage() : new BigDecimal("5.00");
            final BigDecimal commissionRate = (configuredCommRate != null && configuredCommRate.compareTo(BigDecimal.ZERO) >= 0)
                    ? configuredCommRate
                    : new BigDecimal("5.00");

            BigDecimal commissionAmount = safeSubtotal.multiply(commissionRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal sellerPayout = safeSubtotal.subtract(commissionAmount).add(safeShipping).setScale(2, RoundingMode.HALF_UP);

            // 3. Reversal from Seller Wallet
            if (seller != null && sellerPayout.compareTo(BigDecimal.ZERO) > 0) {
                userRepository.findByIdForUpdate(seller.getId()).ifPresent(sellerLocked -> {
                    sellerLocked.debitBalance(sellerPayout);
                    userRepository.save(sellerLocked);

                    WalletTransaction sellerClawbackTx = new WalletTransaction();
                    sellerClawbackTx.setUser(sellerLocked);
                    sellerClawbackTx.setType(WalletTransactionType.PAYMENT);
                    sellerClawbackTx.setAmount(sellerPayout);
                    sellerClawbackTx.setBalanceAfter(sellerLocked.getBalance());
                    sellerClawbackTx.setStatus(WalletTransactionStatus.COMPLETED);
                    sellerClawbackTx.setReference(order.getOrderNumber());
                    sellerClawbackTx.setDescription("Reversal of Order Earnings (Refund): " + order.getOrderNumber());
                    walletTransactionRepository.save(sellerClawbackTx);
                    log.info("Clawed back ${} earnings from Seller {} for cancelled Order {}", sellerPayout, sellerLocked.getUsername(), order.getOrderNumber());
                });
            }

            // 4. Reversal from Platform Admin Wallet
            if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
                userRepository.findFirstByRole(UserRole.ADMIN).ifPresent(adminLocked -> {
                    adminLocked.debitBalance(commissionAmount);
                    userRepository.save(adminLocked);

                    WalletTransaction adminCommReversalTx = new WalletTransaction();
                    adminCommReversalTx.setUser(adminLocked);
                    adminCommReversalTx.setType(WalletTransactionType.FEE);
                    adminCommReversalTx.setAmount(commissionAmount);
                    adminCommReversalTx.setBalanceAfter(adminLocked.getBalance());
                    adminCommReversalTx.setStatus(WalletTransactionStatus.COMPLETED);
                    adminCommReversalTx.setReference(order.getOrderNumber());
                    adminCommReversalTx.setDescription("Reversal of Platform Commission: " + order.getOrderNumber());
                    walletTransactionRepository.save(adminCommReversalTx);
                    log.info("Reversed ${} platform commission for cancelled Order {}", commissionAmount, order.getOrderNumber());
                });
            }
        } else {
            log.info("Order {} was cancelled while in Escrow hold. Buyer refunded from Master Bank escrow without debiting seller.", order.getOrderNumber());
        }

        // 5. Update Escrow and Payment Status to REFUNDED
        order.setEscrowStatus("REFUNDED");
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        if (paymentRepository != null) {
            paymentRepository.save(payment);
        }
    }

    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactionHistory(Long userId, Pageable pageable) {
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> getRecentTransactions(Long userId) {
        return walletTransactionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }
}
