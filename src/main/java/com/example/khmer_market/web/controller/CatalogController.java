package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Category;
import com.example.khmer_market.domain.entity.Product;
import com.example.khmer_market.domain.entity.Review;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.CatalogService;
import com.example.khmer_market.service.CategoryService;
import com.example.khmer_market.service.FavoriteService;
import com.example.khmer_market.service.ReviewService;
import com.example.khmer_market.web.dto.ProductCardDto;
import com.example.khmer_market.web.dto.ProductDetailDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class CatalogController {

    private final CatalogService catalogService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;

    public CatalogController(CatalogService catalogService,
                             CategoryService categoryService,
                             ReviewService reviewService,
                             FavoriteService favoriteService) {
        this.catalogService = catalogService;
        this.categoryService = categoryService;
        this.reviewService = reviewService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/products")
    public String browseCatalog(@RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Long categoryId,
                                @RequestParam(required = false) BigDecimal minPrice,
                                @RequestParam(required = false) BigDecimal maxPrice,
                                @RequestParam(required = false) String province,
                                @RequestParam(defaultValue = "newest") String sort,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(size, 48));
        Page<ProductCardDto> products = catalogService.findProducts(keyword, categoryId, minPrice, maxPrice, province, sort, pageable);

        List<Category> categories = categoryService.findAllActive();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedProvince", province);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());

        return "public/products";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        ProductDetailDto product = catalogService.getProductDetail(id);
        Page<Review> reviews = reviewService.getProductReviews(id, PageRequest.of(0, 10));
        var ratingSummary = reviewService.getProductRatingSummary(id);

        boolean isFavorited = false;
        if (userDetails != null && userDetails.getUser().getRole() == UserRole.BUYER) {
            isFavorited = favoriteService.getUserFavorites(userDetails.getUserId(), PageRequest.of(0, 100))
                    .getContent().stream().anyMatch(f -> f.getId().equals(id));
        }

        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("ratingSummary", ratingSummary);
        model.addAttribute("isFavorited", isFavorited);

        return "public/product-detail";
    }

    @GetMapping("/farmers/{id}")
    public String farmerPublicProfile(@PathVariable Long id,
                                      Model model) {
        var profile = catalogService.getPublicFarmerProfile(id);
        Page<ProductCardDto> products = catalogService.findProducts(null, null, null, null, null, "newest", PageRequest.of(0, 24));
        // Filter by this seller
        List<ProductCardDto> farmerProducts = products.getContent().stream()
                .filter(p -> p.getSellerId().equals(id))
                .toList();

        model.addAttribute("farmer", profile);
        model.addAttribute("products", farmerProducts);

        return "public/farmer-profile";
    }
}
