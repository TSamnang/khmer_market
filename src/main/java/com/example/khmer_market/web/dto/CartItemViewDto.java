package com.example.khmer_market.web.dto;

import java.math.BigDecimal;

public class CartItemViewDto {

    private Long id;
    private Long packageId;
    private String packageName;
    private Long productId;
    private String productName;
    private Long sellerId;
    private String farmName;
    private String imageUrl;
    private BigDecimal unitPrice;
    private BigDecimal oldPrice;
    private int quantity;
    private int availableStock;
    private BigDecimal subtotal;
    private boolean priceChanged;
    private boolean inactive;
    private boolean outOfStock;

    public CartItemViewDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCartItemId() { return id; }
    public void setCartItemId(Long cartItemId) { this.id = cartItemId; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getSellerFarmName() { return farmName; }
    public void setSellerFarmName(String sellerFarmName) { this.farmName = sellerFarmName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getOldPrice() { return oldPrice; }
    public void setOldPrice(BigDecimal oldPrice) { this.oldPrice = oldPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getAvailableStock() { return availableStock; }
    public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public boolean isPriceChanged() { return priceChanged; }
    public void setPriceChanged(boolean priceChanged) { this.priceChanged = priceChanged; }

    public boolean isInactive() { return inactive; }
    public void setInactive(boolean inactive) { this.inactive = inactive; }

    public boolean isOutOfStock() { return outOfStock; }
    public void setOutOfStock(boolean outOfStock) { this.outOfStock = outOfStock; }
}
