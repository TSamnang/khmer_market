package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.FarmerProfile;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.repository.FarmerProfileRepository;
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

@Controller
@RequestMapping("/admin/farmers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFarmerController {

    private final AdminService adminService;
    private final FarmerProfileRepository farmerProfileRepository;

    public AdminFarmerController(AdminService adminService, FarmerProfileRepository farmerProfileRepository) {
        this.adminService = adminService;
        this.farmerProfileRepository = farmerProfileRepository;
    }

    @GetMapping
    public String listFarmers(@RequestParam(required = false) String query,
                              @RequestParam(required = false) String province,
                              @RequestParam(required = false, defaultValue = "PENDING") String status,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "15") int size,
                              Model model) {
        UserStatus userStatus = null;
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            try {
                userStatus = UserStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        Page<FarmerProfile> farmers = adminService.searchFarmerProfiles(query, province, userStatus, pageable);

        model.addAttribute("farmers", farmers);
        model.addAttribute("query", query);
        model.addAttribute("province", province);
        model.addAttribute("selectedStatus", status != null ? status.toUpperCase() : "PENDING");
        model.addAttribute("provinces", farmerProfileRepository.findDistinctProvinces());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", farmers.getTotalPages());
        model.addAttribute("totalElements", farmers.getTotalElements());
        model.addAttribute("activeNav", "farmers");
        return "admin/farmers";
    }

    @GetMapping("/{id}")
    public String viewFarmer(@PathVariable Long id, Model model) {
        FarmerProfile profile = adminService.getFarmerProfile(id);
        model.addAttribute("farmer", profile);
        model.addAttribute("activeNav", "farmers");
        return "admin/farmer-detail";
    }

    @PostMapping("/{id}/approve")
    public String approveFarmer(@PathVariable Long id,
                                java.security.Principal principal,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.approveFarmer(id, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Farmer account approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/farmers";
    }

    @PostMapping("/{id}/reject")
    public String rejectFarmer(@PathVariable Long id,
                               @RequestParam(required = false) String reason,
                               java.security.Principal principal,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.rejectFarmer(id, reason, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Farmer application rejected and marked as suspended.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/farmers";
    }
}
