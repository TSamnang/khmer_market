package com.example.khmer_market.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartItemRequestDto {

    @NotNull(message = "Package ID is required")
    private Long packageId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100, message = "Quantity cannot exceed 100 per item")
    private Integer quantity = 1;

    public CartItemRequestDto() {}

    public CartItemRequestDto(Long packageId, Integer quantity) {
        this.packageId = packageId;
        this.quantity = quantity;
    }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
