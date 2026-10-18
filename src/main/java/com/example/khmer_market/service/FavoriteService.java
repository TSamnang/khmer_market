package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.Favorite;
import com.example.khmer_market.domain.entity.Product;
import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.FavoriteRepository;
import com.example.khmer_market.repository.ProductRepository;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.web.dto.ProductCardDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CatalogService catalogService;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           CatalogService catalogService) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.catalogService = catalogService;
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long productId) {
        Optional<Favorite> existingOpt = favoriteRepository.findByUserIdAndProductId(userId, productId);
        if (existingOpt.isPresent()) {
            favoriteRepository.delete(existingOpt.get());
            return false;
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

            Favorite favorite = new Favorite(user, product);
            favoriteRepository.save(favorite);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public Page<ProductCardDto> getUserFavorites(Long userId, Pageable pageable) {
        Page<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<ProductCardDto> cards = favorites.getContent().stream()
                .filter(f -> f.getProduct().isActive())
                .map(f -> {
                    ProductCardDto card = new ProductCardDto();
                    Product p = f.getProduct();
                    card.setId(p.getId());
                    card.setName(p.getName());
                    card.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : "");
                    if (p.getSeller() != null) {
                        card.setSellerId(p.getSeller().getId());
                        card.setSellerUsername(p.getSeller().getUsername());
                        String fullName = p.getSeller().getFullName();
                        if (p.getSeller().getFarmerProfile() != null) {
                            card.setFarmName(p.getSeller().getFarmerProfile().getFarmName());
                            card.setProvince(p.getSeller().getFarmerProfile().getProvince());
                            card.setSellerName(p.getSeller().getFarmerProfile().getFarmerName() != null && !p.getSeller().getFarmerProfile().getFarmerName().isBlank()
                                    ? p.getSeller().getFarmerProfile().getFarmerName() : fullName);
                        } else {
                            card.setSellerName(fullName != null && !fullName.isBlank() ? fullName : p.getSeller().getUsername());
                        }
                    }
                    if (p.getPrimaryImage() != null) {
                        card.setPrimaryImageUrl(p.getPrimaryImage().getImageUrl());
                    }
                    return card;
                })
                .toList();

        return new PageImpl<>(cards, pageable, favorites.getTotalElements());
    }
}
