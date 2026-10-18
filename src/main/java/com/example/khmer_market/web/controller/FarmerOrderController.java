package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/farmer/orders")
public class FarmerOrderController {

    private final OrderService orderService;

    public FarmerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String listOrders(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                orderStatus = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        Page<Order> orders = orderService.getFarmerOrders(userDetails.getUserId(), orderStatus, pageable);

        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());

        return "farmer/orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        Order order = orderService.getFarmerOrderDetail(id, userDetails.getUserId());
        model.addAttribute("order", order);
        return "farmer/order-detail";
    }

    @PostMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               jakarta.servlet.http.HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.confirmOrderAsFarmer(id, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Order confirmed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/farmer/dashboard")) {
            return "redirect:/farmer/dashboard";
        }
        return "redirect:/farmer/orders/" + id;
    }

    @PostMapping("/{id}/ship")
    public String shipOrder(@PathVariable Long id,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            jakarta.servlet.http.HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            orderService.shipOrderAsFarmer(id, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Order marked as shipped");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/farmer/dashboard")) {
            return "redirect:/farmer/dashboard";
        }
        return "redirect:/farmer/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam(value = "reason", defaultValue = "Cancelled by seller") String reason,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrderAsFarmer(id, userDetails.getUserId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Order cancelled and inventory restored");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/farmer/orders/" + id;
    }
}
