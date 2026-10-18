package com.example.khmer_market.web.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class CartViewDto {

    private Long id;
    private List<CartItemViewDto> items = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal shippingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal grandTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private int totalItemCount = 0;
    private boolean priceChanged = false;
    private boolean hasInactiveOrOutOfStockItems = false;

    public CartViewDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<CartItemViewDto> getItems() { return items; }
    public void setItems(List<CartItemViewDto> items) { this.items = items; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getShippingFee() {
        return shippingFee != null ? shippingFee : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee != null ? shippingFee.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getGrandTotal() {
        if (grandTotal != null && grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            return grandTotal;
        }
        BigDecimal sub = subtotal != null ? subtotal : BigDecimal.ZERO;
        BigDecimal ship = shippingFee != null ? shippingFee : BigDecimal.ZERO;
        return sub.add(ship).setScale(2, RoundingMode.HALF_UP);
    }
    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal != null ? grandTotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public int getTotalItemCount() { return totalItemCount; }
    public void setTotalItemCount(int totalItemCount) { this.totalItemCount = totalItemCount; }

    public boolean isPriceChanged() { return priceChanged; }
    public void setPriceChanged(boolean priceChanged) { this.priceChanged = priceChanged; }

    public boolean isHasInactiveOrOutOfStockItems() { return hasInactiveOrOutOfStockItems; }
    public boolean getHasInactiveOrOutOfStockItems() { return hasInactiveOrOutOfStockItems; }
    public boolean hasInactiveOrOutOfStockItems() { return hasInactiveOrOutOfStockItems; }
    public void setHasInactiveOrOutOfStockItems(boolean hasInactiveOrOutOfStockItems) { this.hasInactiveOrOutOfStockItems = hasInactiveOrOutOfStockItems; }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean getEmpty() {
        return isEmpty();
    }
}
