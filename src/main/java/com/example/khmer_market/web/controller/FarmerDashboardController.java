package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.FarmerService;
import com.example.khmer_market.service.OrderService;
import com.example.khmer_market.service.ProductService;
import com.example.khmer_market.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/farmer")
public class FarmerDashboardController {

    private final FarmerService farmerService;
    private final OrderService orderService;
    private final ProductService productService;
    private final WalletService walletService;

    public FarmerDashboardController(FarmerService farmerService,
                                     OrderService orderService,
                                     ProductService productService,
                                     WalletService walletService) {
        this.farmerService = farmerService;
        this.orderService = orderService;
        this.productService = productService;
        this.walletService = walletService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long sellerId = userDetails.getUserId();
        var stats = farmerService.getDashboardStats(sellerId);
        Page<Order> recentOrders = orderService.getFarmerOrders(sellerId, null, PageRequest.of(0, 5));

        model.addAttribute("stats", stats);
        model.addAttribute("recentOrders", recentOrders.getContent());
        model.addAttribute("farmerProfile", farmerService.getProfile(sellerId));
        model.addAttribute("farmerBalance", walletService.getBalance(sellerId));

        return "farmer/dashboard";
    }

    @PostMapping("/packages/{packageId}/inventory")
    public String updateInventory(@PathVariable Long packageId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam int quantity,
                                  @RequestParam(defaultValue = "5") int lowStockThreshold,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            productService.updateInventory(packageId, userDetails.getUserId(), quantity, null);
            redirectAttributes.addFlashAttribute("successMessage", "Stock successfully updated to " + quantity + " units!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        String referer = request.getHeader("Referer");
        if (referer != null && (referer.contains("/farmer/products") || referer.contains("/edit"))) {
            return "redirect:" + referer;
        }
        return "redirect:/farmer/dashboard";
    }
}
