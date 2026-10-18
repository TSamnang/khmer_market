package com.example.khmer_market.web.dto;

import java.math.BigDecimal;

public class ProductCardDto {

    private Long id;
    private String name;
    private String primaryImageUrl;
    private String categoryName;
    private String farmName;
    private String province;
    private BigDecimal fromPrice;
    private boolean inStock;
    private double averageRating;
    private long reviewCount;
    private Long sellerId;
    private String sellerUsername;
    private String sellerName;

    public ProductCardDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrimaryImageUrl() { return primaryImageUrl; }
    public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public BigDecimal getFromPrice() { return fromPrice; }
    public void setFromPrice(BigDecimal fromPrice) { this.fromPrice = fromPrice; }

    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getSellerUsername() {
        if (sellerUsername != null && !sellerUsername.isBlank()) {
            return sellerUsername;
        }
        return "seller" + (sellerId != null ? sellerId : "");
    }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

    public String getSellerName() {
        if (sellerName != null && !sellerName.isBlank()) {
            return sellerName;
        }
        if (farmName != null && !farmName.isBlank()) {
            return farmName;
        }
        return getSellerUsername();
    }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getFarmerName() { return getSellerName(); }
    public void setFarmerName(String farmerName) { this.sellerName = farmerName; }

    public BigDecimal getMinPrice() { return fromPrice != null ? fromPrice : BigDecimal.ZERO; }
    public String getSellerFarmName() { return farmName; }
}
