package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.model.StoreConfig;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.CartService;
import com.example.khmer_market.service.StoreConfigService;
import com.example.khmer_market.service.WalletService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ControllerAdvice
public class GlobalStoreAdvice {

    private final StoreConfigService storeConfigService;
    private final CartService cartService;
    private final WalletService walletService;

    public GlobalStoreAdvice(StoreConfigService storeConfigService,
                             CartService cartService,
                             WalletService walletService) {
        this.storeConfigService = storeConfigService;
        this.cartService = cartService;
        this.walletService = walletService;
    }

    @ModelAttribute("storeConfig")
    public StoreConfig populateStoreConfig() {
        return storeConfigService.getConfig();
    }

    @ModelAttribute("cartItemCount")
    public int populateCartItemCount() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return cartService.getCartItemCount(userDetails.getUserId());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    @ModelAttribute("currentUserBalance")
    public BigDecimal populateCurrentUserBalance() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return walletService.getBalance(userDetails.getUserId());
            }
        } catch (Exception ignored) {
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
