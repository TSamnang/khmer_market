package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.model.StoreConfig;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.CartService;
import com.example.khmer_market.service.StoreConfigService;
import com.example.khmer_market.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.stream.Collectors;

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

    @ModelAttribute("currentUrl")
    public String populateCurrentUrl(HttpServletRequest request) {
        if (request == null) {
            return "/";
        }
        Object forwardUri = request.getAttribute("jakarta.servlet.forward.request_uri");
        String uri = (forwardUri instanceof String s && !s.isBlank()) ? s : request.getRequestURI();
        if (uri == null || uri.isBlank() || uri.endsWith(".svg") || uri.endsWith(".png") || uri.endsWith(".jpg") || uri.startsWith("/images/") || uri.startsWith("/css/") || uri.startsWith("/js/")) {
            uri = "/";
        }
        Object forwardQuery = request.getAttribute("jakarta.servlet.forward.query_string");
        String query = (forwardQuery instanceof String q && !q.isBlank()) ? q : request.getQueryString();
        if (query != null && !query.isBlank()) {
            String cleanedQuery = Arrays.stream(query.split("&"))
                    .filter(param -> !param.startsWith("lang=") && !param.isBlank())
                    .collect(Collectors.joining("&"));
            if (!cleanedQuery.isBlank()) {
                return uri + "?" + cleanedQuery;
            }
        }
        return uri;
    }
}
