package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.entity.Payment;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.repository.PaymentRepository;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminService adminService;
    private final PaymentRepository paymentRepository;

    public AdminOrderController(AdminService adminService, PaymentRepository paymentRepository) {
        this.adminService = adminService;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping
    public String listOrders(@RequestParam(required = false) String query,
                             @RequestParam(required = false) OrderStatus status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "15") int size,
                             Model model) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        Page<Order> orders = adminService.searchOrders(query, status, pageable);

        model.addAttribute("orders", orders);
        model.addAttribute("query", query);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        model.addAttribute("totalElements", orders.getTotalElements());
        model.addAttribute("activeNav", "orders");
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        Order order = adminService.getOrder(id);
        List<Payment> payments = paymentRepository.findByOrderId(id);

        model.addAttribute("order", order);
        model.addAttribute("payments", payments);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("activeNav", "orders");
        return "admin/order-detail";
    }

    @PostMapping("/{id}/override-status")
    public String overrideStatus(@PathVariable Long id,
                                 @RequestParam OrderStatus targetStatus,
                                 @RequestParam(required = false, defaultValue = "Administrative override") String reason,
                                 java.security.Principal principal,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.overrideOrderStatus(id, targetStatus, reason, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Order status successfully transitioned to " + targetStatus.name());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
