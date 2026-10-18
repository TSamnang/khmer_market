package com.example.khmer_market.web.controller;

import com.example.khmer_market.service.AdminService;
import com.example.khmer_market.service.AuditService;
import com.example.khmer_market.service.RateLimitService;
import com.example.khmer_market.service.RateLimitService.RateLimitPolicy;
import com.example.khmer_market.web.dto.ChangePasswordDto;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final AdminService adminService;
    private final AuditService auditService;
    private final RateLimitService rateLimitService;

    public AdminSettingsController(AdminService adminService,
                                  AuditService auditService,
                                  RateLimitService rateLimitService) {
        this.adminService = adminService;
        this.auditService = auditService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public String viewSettings(Model model) {
        model.addAttribute("primaryPolicies", rateLimitService.getPrimaryPolicies());
        model.addAttribute("additionalPolicies", rateLimitService.getAdditionalPolicies());
        model.addAttribute("allPolicies", rateLimitService.getAllPolicies());
        model.addAttribute("recentAudits", auditService.getRecentEvents());
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        model.addAttribute("activeNav", "settings");
        return "admin/settings";
    }

    @PostMapping("/rate-limit")
    public String updateRateLimit(@RequestParam("policyId") String policyId,
                                  @RequestParam("capacity") long capacity,
                                  @RequestParam("periodSeconds") long periodSeconds,
                                  Principal principal,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            RateLimitPolicy updated = rateLimitService.updatePolicy(
                    policyId, capacity, periodSeconds, adminUsername, request.getRemoteAddr()
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Rate limit policy '" + updated.getName() + "' updated to " + updated.getLimitFormatted() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update rate limit: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/rate-limit/reset")
    public String resetRateLimits(Principal principal,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            rateLimitService.resetToDefaults(adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "All rate limiting policies have been reset to factory defaults.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reset rate limits: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDto") ChangePasswordDto dto,
                                 BindingResult bindingResult,
                                 Principal principal,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            return viewSettings(model);
        }

        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.changeAdminPassword(adminUsername, dto, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Admin password successfully updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/settings";
    }
}
