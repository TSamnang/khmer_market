package com.example.khmer_market.web.controller;

import com.example.khmer_market.service.AdminService;
import com.example.khmer_market.service.AuditService;
import com.example.khmer_market.service.StoreConfigService;
import com.example.khmer_market.web.dto.StoreConfigDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminService adminService;
    private final AuditService auditService;
    private final StoreConfigService storeConfigService;

    public AdminDashboardController(AdminService adminService,
                                  AuditService auditService,
                                  StoreConfigService storeConfigService) {
        this.adminService = adminService;
        this.auditService = auditService;
        this.storeConfigService = storeConfigService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("stats", adminService.getDashboardStats());
        model.addAttribute("recentAudits", auditService.getRecentEvents());
        model.addAttribute("storeConfigDto", StoreConfigDto.fromConfig(storeConfigService.getConfig()));
        model.addAttribute("activeNav", "dashboard");
        return "admin/dashboard";
    }

    @PostMapping("/dashboard/quick-customize")
    public String quickCustomize(@ModelAttribute("storeConfigDto") StoreConfigDto dto,
                                 BindingResult bindingResult,
                                 Principal principal,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (dto.getStoreName() == null || dto.getStoreName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Store name cannot be empty.");
            return "redirect:/admin/dashboard";
        }
        if (dto.getExchangeRateUsdKhr() == null || dto.getExchangeRateUsdKhr().compareTo(new java.math.BigDecimal("100.00")) < 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Exchange rate must be at least 100 KHR per USD.");
            return "redirect:/admin/dashboard";
        }
        if (dto.getCommissionPercentage() != null && (dto.getCommissionPercentage().compareTo(java.math.BigDecimal.ZERO) < 0 || dto.getCommissionPercentage().compareTo(new java.math.BigDecimal("100.00")) > 0)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Commission rate must be between 0% and 100%.");
            return "redirect:/admin/dashboard";
        }
        if (dto.getWithdrawalFee() != null && dto.getWithdrawalFee().compareTo(java.math.BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Withdrawal fee cannot be negative.");
            return "redirect:/admin/dashboard";
        }

        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            storeConfigService.updateConfig(dto, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Storefront customization updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update store settings: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }
}
