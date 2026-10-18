package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.PackageStatus;
import com.example.khmer_market.domain.enums.ReviewStatus;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.web.dto.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CatalogService {

    private final ProductRepository productRepository;
    private final ProductPackageRepository productPackageRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final UserRepository userRepository;

    public CatalogService(ProductRepository productRepository,
                          ProductPackageRepository productPackageRepository,
                          ReviewRepository reviewRepository,
                          FavoriteRepository favoriteRepository,
                          FarmerProfileRepository farmerProfileRepository,
                          UserRepository userRepository) {
        this.productRepository = productRepository;
        this.productPackageRepository = productPackageRepository;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.farmerProfileRepository = farmerProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getFeaturedProducts(int limit) {
        ProductFilterDto filter = new ProductFilterDto();
        filter.setPage(0);
        filter.setSize(limit);
        return findCatalogProducts(filter).getContent();
    }

    @Transactional(readOnly = true)
    public Page<ProductCardDto> findProducts(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String province, String sort, Pageable pageable) {
        ProductFilterDto filter = new ProductFilterDto();
        filter.setQuery(keyword);
        filter.setCategoryId(categoryId);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setProvince(province);
        filter.setSort(sort);
        filter.setPage(pageable.getPageNumber());
        filter.setSize(pageable.getPageSize());
        return findCatalogProducts(filter);
    }

    @Transactional(readOnly = true)
    public ProductDetailDto getProductDetail(Long productId) {
        return getProductDetail(productId, null);
    }

    @Transactional(readOnly = true)
    public FarmerProfileDto getPublicFarmerProfile(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + sellerId));

        FarmerProfile profile = farmerProfileRepository.findByUserId(sellerId).orElse(null);
        FarmerProfileDto dto = new FarmerProfileDto();
        dto.setUsername(seller.getUsername());
        if (profile != null) {
            dto.setFarmName(profile.getFarmName());
            dto.setFarmerName(profile.getFarmerName());
            dto.setPhone(profile.getPhone());
            dto.setProvince(profile.getProvince());
            dto.setAddress(profile.getAddress());
            dto.setFarmDescription(profile.getFarmDescription());
            dto.setFarmImage(profile.getFarmImage());
        } else {
            dto.setFarmName(seller.getFullName() != null ? seller.getFullName() : seller.getUsername());
            dto.setFarmerName(seller.getFullName());
            dto.setPhone(seller.getPhone());
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<ProductCardDto> findCatalogProducts(ProductFilterDto filter) {
        int page = filter.getPage();
        int size = Math.min(50, filter.getSize());

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> productPage = productRepository.findPublicCatalogProducts(
                filter.getQuery() != null && !filter.getQuery().isBlank() ? filter.getQuery().trim() : null,
                filter.getCategoryId(),
                filter.getProvince() != null && !filter.getProvince().isBlank() ? filter.getProvince().trim() : null,
                null,
                pageable
        );

        List<ProductCardDto> dtos = new ArrayList<>();
        for (Product product : productPage.getContent()) {
            ProductCardDto card = toCardDto(product);

            // Filter in-stock only if requested
            if (Boolean.TRUE.equals(filter.getInStockOnly()) && !card.isInStock()) {
                continue;
            }

            // Filter by price range if provided
            if (filter.getMinPrice() != null && card.getFromPrice() != null && card.getFromPrice().compareTo(filter.getMinPrice()) < 0) {
                continue;
            }
            if (filter.getMaxPrice() != null && card.getFromPrice() != null && card.getFromPrice().compareTo(filter.getMaxPrice()) > 0) {
                continue;
            }

            dtos.add(card);
        }

        // Apply custom in-memory sorting when required (e.g. price or rating)
        if ("price_asc".equalsIgnoreCase(filter.getSort())) {
            dtos.sort(Comparator.comparing(ProductCardDto::getFromPrice, Comparator.nullsLast(Comparator.naturalOrder())));
        } else if ("price_desc".equalsIgnoreCase(filter.getSort())) {
            dtos.sort(Comparator.comparing(ProductCardDto::getFromPrice, Comparator.nullsLast(Comparator.reverseOrder())));
        } else if ("rating".equalsIgnoreCase(filter.getSort())) {
            dtos.sort(Comparator.comparing(ProductCardDto::getAverageRating, Comparator.reverseOrder()));
        }

        return new PageImpl<>(dtos, pageable, productPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductDetailDto getProductDetail(Long productId, Long currentUserId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        if (!product.isActive() || !product.getSeller().isActive()) {
            throw new ResourceNotFoundException("This product is no longer active");
        }

        ProductDetailDto dto = new ProductDetailDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setSellerId(product.getSeller().getId());
        dto.setSellerUsername(product.getSeller().getUsername());

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }

        FarmerProfile farm = product.getSeller().getFarmerProfile();
        if (farm != null) {
            dto.setFarmName(farm.getFarmName());
            dto.setFarmerName(farm.getFarmerName());
            dto.setProvince(farm.getProvince());
            dto.setFarmAddress(farm.getAddress());
            dto.setFarmDescription(farm.getFarmDescription());
            dto.setFarmImage(farm.getFarmImage());
        }

        List<String> images = new ArrayList<>();
        String primary = null;
        for (ProductImage img : product.getImages()) {
            images.add(img.getImageUrl());
            if (img.isPrimary()) {
                primary = img.getImageUrl();
            }
        }
        if (primary == null && !images.isEmpty()) {
            primary = images.get(0);
        }
        dto.setImageUrls(images);
        dto.setPrimaryImageUrl(primary);

        // Active packages with stock
        List<PackageDetailDto> packages = new ArrayList<>();
        for (ProductPackage pkg : product.getPackages()) {
            if (pkg.getStatus() == PackageStatus.ACTIVE) {
                PackageDetailDto pkgDto = new PackageDetailDto();
                pkgDto.setId(pkg.getId());
                pkgDto.setPackageName(pkg.getPackageName());
                pkgDto.setUnit(pkg.getUnit());
                pkgDto.setUnitSize(pkg.getUnitSize());
                pkgDto.setPrice(pkg.getPrice());
                pkgDto.setSku(pkg.getSku());

                Inventory inv = pkg.getInventory();
                int available = (inv != null) ? inv.getAvailableQuantity() : 0;
                pkgDto.setAvailableQuantity(available);
                pkgDto.setInStock(available > 0);
                packages.add(pkgDto);
            }
        }
        dto.setPackages(packages);

        // Reviews
        Double avgRating = reviewRepository.getAverageRatingForProduct(productId);
        dto.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        dto.setReviewCount(reviewRepository.countPublishedReviewsForProduct(productId));

        Page<Review> reviewPage = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                productId, ReviewStatus.PUBLISHED, PageRequest.of(0, 10));
        List<ReviewDto> reviewDtos = new ArrayList<>();
        for (Review r : reviewPage.getContent()) {
            ReviewDto rd = new ReviewDto();
            rd.setId(r.getId());
            rd.setRating(r.getRating());
            rd.setComment(r.getComment());
            rd.setCreatedAt(r.getCreatedAt());
            rd.setBuyerUsername(r.getUser().getUsername());
            reviewDtos.add(rd);
        }
        dto.setReviews(reviewDtos);

        if (currentUserId != null) {
            dto.setFavorite(favoriteRepository.existsByUserIdAndProductId(currentUserId, productId));
        }

        return dto;
    }

    private ProductCardDto toCardDto(Product product) {
        ProductCardDto dto = new ProductCardDto();
        dto.setId(product.getId());
        dto.setName(product.getName());

        if (product.getSeller() != null) {
            dto.setSellerId(product.getSeller().getId());
            dto.setSellerUsername(product.getSeller().getUsername());
            String fullName = product.getSeller().getFullName();

            FarmerProfile farm = product.getSeller().getFarmerProfile();
            if (farm != null) {
                dto.setFarmName(farm.getFarmName());
                dto.setProvince(farm.getProvince());
                dto.setSellerName(farm.getFarmerName() != null && !farm.getFarmerName().isBlank()
                        ? farm.getFarmerName() : fullName);
            } else {
                dto.setSellerName(fullName != null && !fullName.isBlank() ? fullName : product.getSeller().getUsername());
            }
        }

        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }

        ProductImage primaryImg = product.getPrimaryImage();
        if (primaryImg != null) {
            dto.setPrimaryImageUrl(primaryImg.getImageUrl());
        }

        BigDecimal minPrice = productPackageRepository.findMinActivePriceByProductId(product.getId());
        dto.setFromPrice(minPrice != null ? minPrice : BigDecimal.ZERO);
        dto.setInStock(productPackageRepository.isProductInStock(product.getId()));

        Double avgRating = reviewRepository.getAverageRatingForProduct(product.getId());
        dto.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        dto.setReviewCount(reviewRepository.countPublishedReviewsForProduct(product.getId()));

        return dto;
    }
}
