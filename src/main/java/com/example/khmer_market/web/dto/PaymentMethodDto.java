package com.example.khmer_market.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PaymentMethodDto {

    private Long id;

    @NotBlank(message = "Payment method type is required")
    private String methodType = "BANK_ACCOUNT"; // 'BANK_ACCOUNT', 'CARD', 'BAKONG'

    @NotBlank(message = "Provider name is required")
    @Size(max = 60, message = "Provider must not exceed 60 characters")
    private String provider = "ABA Bank";

    @NotBlank(message = "Account number or Card number is required")
    @Size(max = 100, message = "Account number must not exceed 100 characters")
    private String accountNumber;

    @NotBlank(message = "Account holder name is required")
    @Size(max = 150, message = "Account holder name must not exceed 150 characters")
    private String accountHolderName;

    @Size(max = 20, message = "Card expiry must not exceed 20 characters")
    private String cardExpiry;

    private boolean isDefault = false;

    public PaymentMethodDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getCardExpiry() {
        return cardExpiry;
    }

    public void setCardExpiry(String cardExpiry) {
        this.cardExpiry = cardExpiry;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
