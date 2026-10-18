package com.example.khmer_market.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CheckoutDto {

    private Long addressId;

    // Validated conditionally in CheckoutController when addressId is null
    private AddressDto newAddress;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(balance|card|khqr)$", message = "Invalid payment method")
    private String paymentMethod = "balance";

    @NotBlank(message = "Security token is required")
    private String checkoutToken;

    // Optional Credit / Debit Card input fields
    private String cardNumber;
    private String cardHolderName;
    private String cardExpiry;
    private String cardCvv;

    public CheckoutDto() {}

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public AddressDto getNewAddress() { return newAddress; }
    public void setNewAddress(AddressDto newAddress) { this.newAddress = newAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCheckoutToken() { return checkoutToken; }
    public void setCheckoutToken(String checkoutToken) { this.checkoutToken = checkoutToken; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public String getCardExpiry() { return cardExpiry; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }

    public String getCardCvv() { return cardCvv; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }
}
