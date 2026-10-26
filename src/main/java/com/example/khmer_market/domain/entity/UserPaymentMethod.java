package com.example.khmer_market.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_payment_methods")
public class UserPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "method_type", length = 30, nullable = false)
    private String methodType; // 'BANK_ACCOUNT', 'CARD', 'BAKONG'

    @Column(name = "provider", length = 60, nullable = false)
    private String provider; // 'ABA Bank', 'ACLEDA Bank', 'Wing Bank', 'Canadia Bank', 'Bakong', 'Visa', 'Mastercard'

    @Column(name = "account_number", length = 100, nullable = false)
    private String accountNumber;

    @Column(name = "account_holder_name", length = 150, nullable = false)
    private String accountHolderName;

    @Column(name = "card_expiry", length = 20)
    private String cardExpiry;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public UserPaymentMethod() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMethodType() { return methodType; }
    public void setMethodType(String methodType) { this.methodType = methodType; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getCardExpiry() { return cardExpiry; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }

    public boolean isDefault() { return isDefault; }
    public boolean getIsDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
    public void setIsDefault(boolean aDefault) { isDefault = aDefault; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.isBlank()) return "";
        String clean = accountNumber.replaceAll("\\s+", "");
        if (clean.length() <= 4) return clean;
        return "**** " + clean.substring(clean.length() - 4);
    }

    public String getBadgeClass() {
        if (provider == null) return "bg-secondary";
        String p = provider.toLowerCase();
        if (p.contains("aba")) return "bg-primary";
        if (p.contains("acleda")) return "bg-info text-dark";
        if (p.contains("wing")) return "bg-success";
        if (p.contains("bakong")) return "bg-danger";
        if (p.contains("visa")) return "bg-primary-subtle text-primary";
        if (p.contains("mastercard")) return "bg-warning-subtle text-dark";
        return "bg-secondary";
    }

    public String getIconClass() {
        if ("CARD".equalsIgnoreCase(methodType) || (provider != null && (provider.toLowerCase().contains("visa") || provider.toLowerCase().contains("mastercard") || provider.toLowerCase().contains("card")))) {
            return "bi-credit-card-2-front";
        }
        if ("BAKONG".equalsIgnoreCase(methodType) || (provider != null && provider.toLowerCase().contains("bakong"))) {
            return "bi-qr-code";
        }
        return "bi-bank";
    }
}
