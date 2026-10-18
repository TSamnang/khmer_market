package com.example.khmer_market.web.controller;

import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.CartService;
import com.example.khmer_market.web.dto.CartViewDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String viewCart(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        CartViewDto cartView = cartService.getCartView(userDetails.getUserId());
        model.addAttribute("cart", cartView);
        return "buyer/cart";
    }

    @PostMapping("/cart/add")
    public Object addToCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam(required = false) Long packageId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (userDetails == null) {
            if (isAjax) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Please sign in to add items to cart"));
            }
            return "redirect:/login";
        }

        if (packageId == null) {
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Please select a packaging option"));
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a packaging option");
            String referer = request.getHeader("Referer");
            return referer != null ? "redirect:" + referer : "redirect:/products";
        }

        int qty = (quantity != null && quantity > 0) ? quantity : 1;

        try {
            cartService.addToCart(userDetails.getUserId(), packageId, qty);
            int newTotalCount = cartService.getCartItemCount(userDetails.getUserId());
            if (isAjax) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "cartItemCount", newTotalCount,
                        "message", "Added to cart successfully"
                ));
            }
            redirectAttributes.addFlashAttribute("successMessage", "Added to cart successfully");
        } catch (Exception e) {
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage() != null ? e.getMessage() : "Unable to add item to cart"));
            }
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam(required = false) Long cartItemId,
                                 @RequestParam(defaultValue = "1") Integer quantity,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (cartItemId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid cart item specified");
            return "redirect:/cart";
        }
        int qty = (quantity != null) ? quantity : 1;
        try {
            cartService.updateItemQuantity(userDetails.getUserId(), cartItemId, qty);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeItem(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam(required = false) Long cartItemId,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (cartItemId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid cart item specified");
            return "redirect:/cart";
        }
        try {
            cartService.removeItem(userDetails.getUserId(), cartItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    @GetMapping("/api/cart/count")
    @ResponseBody
    public Map<String, Object> getCartCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        int count = (userDetails != null) ? cartService.getCartItemCount(userDetails.getUserId()) : 0;
        return Map.of("count", count);
    }
}
