package com.example.khmer_market.web.controller;

import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.FavoriteService;
import com.example.khmer_market.web.dto.ProductCardDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/buyer/favorites")
public class BuyerFavoriteController {

    private final FavoriteService favoriteService;

    public BuyerFavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public String listFavorites(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size,
                                Model model) {
        Page<ProductCardDto> favorites = favoriteService.getUserFavorites(userDetails.getUserId(), PageRequest.of(Math.max(0, page), size));
        model.addAttribute("favorites", favorites);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", favorites.getTotalPages());
        return "buyer/favorites";
    }

    @PostMapping("/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleFavorite(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                              @RequestParam Long productId) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        boolean favorited = favoriteService.toggleFavorite(userDetails.getUserId(), productId);
        return ResponseEntity.ok(Map.of("productId", productId, "favorited", favorited));
    }
}
