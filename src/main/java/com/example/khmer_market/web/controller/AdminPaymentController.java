package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Payment;
import com.example.khmer_market.domain.enums.PaymentMethod;
import com.example.khmer_market.domain.enums.PaymentStatus;
import com.example.khmer_market.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final AdminService adminService;

    public AdminPaymentController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String listPayments(@RequestParam(required = false) String query,
                               @RequestParam(required = false) PaymentMethod method,
                               @RequestParam(required = false) PaymentStatus status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               Model model) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        Page<Payment> payments = adminService.searchPayments(query, method, status, pageable);

        model.addAttribute("payments", payments);
        model.addAttribute("query", query);
        model.addAttribute("selectedMethod", method);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", payments.getTotalPages());
        model.addAttribute("totalElements", payments.getTotalElements());
        model.addAttribute("activeNav", "payments");
        return "admin/payments";
    }

    @GetMapping("/{id}")
    public String viewPayment(@PathVariable Long id, Model model) {
        Payment payment = adminService.getPayment(id);
        model.addAttribute("payment", payment);
        model.addAttribute("activeNav", "payments");
        return "admin/payment-detail";
    }
}
