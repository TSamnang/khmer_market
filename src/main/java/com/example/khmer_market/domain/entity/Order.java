package com.example.khmer_market.domain.entity;

import com.example.khmer_market.domain.enums.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Column(name = "order_number", length = 50, nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "shipping_fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal shippingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "status", length = 30, nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "escrow_status", length = 30, nullable = false)
    private String escrowStatus = "PENDING_PAYMENT";

    @Column(name = "escrow_release_at")
    private LocalDateTime escrowReleaseAt;

    @Column(name = "escrow_released_at")
    private LocalDateTime escrowReleasedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<Payment> payments = new ArrayList<>();

    public Order() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getBuyer() { return buyer; }
    public void setBuyer(User buyer) { this.buyer = buyer; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee != null ? shippingFee.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setOrder(this);
    }

    public User getSeller() {
        if (items != null && !items.isEmpty() && items.get(0).getSeller() != null) {
            return items.get(0).getSeller();
        }
        return null;
    }

    public String getSellerFarmName() {
        User s = getSeller();
        if (s != null) {
            if (s.getFarmerProfile() != null && s.getFarmerProfile().getFarmName() != null && !s.getFarmerProfile().getFarmName().isBlank()) {
                return s.getFarmerProfile().getFarmName();
            }
            if (s.getFullName() != null && !s.getFullName().isBlank()) {
                return s.getFullName();
            }
            return s.getUsername();
        }
        return "Angkor Organic Mart Farm";
    }

    public Payment getPrimaryPayment() {
        if (payments != null && !payments.isEmpty()) {
            return payments.get(0);
        }
        return null;
    }

    public String getEscrowStatus() {
        return escrowStatus;
    }

    public void setEscrowStatus(String escrowStatus) {
        this.escrowStatus = escrowStatus;
    }

    public LocalDateTime getEscrowReleaseAt() {
        return escrowReleaseAt;
    }

    public void setEscrowReleaseAt(LocalDateTime escrowReleaseAt) {
        this.escrowReleaseAt = escrowReleaseAt;
    }

    public LocalDateTime getEscrowReleasedAt() {
        return escrowReleasedAt;
    }

    public void setEscrowReleasedAt(LocalDateTime escrowReleasedAt) {
        this.escrowReleasedAt = escrowReleasedAt;
    }

    public boolean isUnderEscrowHold() {
        if ("HELD".equalsIgnoreCase(escrowStatus)) {
            return escrowReleaseAt == null || LocalDateTime.now().isBefore(escrowReleaseAt);
        }
        return false;
    }

    public boolean isEscrowReleased() {
        return "RELEASED".equalsIgnoreCase(escrowStatus);
    }
}
