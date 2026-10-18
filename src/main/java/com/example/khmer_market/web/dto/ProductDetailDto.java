package com.example.khmer_market.web.dto;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailDto {

    private Long id;
    private String name;
    private String description;
    private String categoryName;
    private Long categoryId;

    // Farm & Seller details
    private Long sellerId;
    private String farmName;
    private String farmerName;
    private String province;
    private String farmAddress;
    private String farmDescription;
    private String farmImage;

    // Images
    private List<String> imageUrls = new ArrayList<>();
    private String primaryImageUrl;

    // Packages & Inventory
    private List<PackageDetailDto> packages = new ArrayList<>();

    // Reviews & Ratings
    private double averageRating;
    private long reviewCount;
    private List<ReviewDto> reviews = new ArrayList<>();

    private boolean favorite;

    public ProductDetailDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getFarmAddress() { return farmAddress; }
    public void setFarmAddress(String farmAddress) { this.farmAddress = farmAddress; }

    public String getFarmDescription() { return farmDescription; }
    public void setFarmDescription(String farmDescription) { this.farmDescription = farmDescription; }

    public String getFarmImage() { return farmImage; }
    public void setFarmImage(String farmImage) { this.farmImage = farmImage; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getPrimaryImageUrl() { return primaryImageUrl; }
    public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }

    public List<PackageDetailDto> getPackages() { return packages; }
    public void setPackages(List<PackageDetailDto> packages) { this.packages = packages; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }

    public List<ReviewDto> getReviews() { return reviews; }
    public void setReviews(List<ReviewDto> reviews) { this.reviews = reviews; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    private String sellerUsername;

    public List<String> getImages() { return imageUrls; }
    public String getSellerFarmName() { return farmName; }
    public String getSellerUsername() {
        if (sellerUsername != null && !sellerUsername.isBlank()) {
            return sellerUsername;
        }
        return farmName != null ? farmName : "Seller " + sellerId;
    }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }
}
