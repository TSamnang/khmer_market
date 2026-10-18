package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Review;
import com.example.khmer_market.domain.enums.ReviewStatus;
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
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final AdminService adminService;

    public AdminReviewController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String listReviews(@RequestParam(required = false) String query,
                              @RequestParam(required = false) Integer rating,
                              @RequestParam(required = false) ReviewStatus status,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              Model model) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        Page<Review> reviews = adminService.searchReviews(query, rating, status, pageable);

        model.addAttribute("reviews", reviews);
        model.addAttribute("query", query);
        model.addAttribute("selectedRating", rating);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ReviewStatus.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviews.getTotalPages());
        model.addAttribute("totalElements", reviews.getTotalElements());
        model.addAttribute("activeNav", "reviews");
        return "admin/reviews";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam ReviewStatus status,
                               java.security.Principal principal,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.updateReviewStatus(id, status, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Review status set to " + status.name());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                               java.security.Principal principal,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.deleteReview(id, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Review permanently deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/reviews";
    }
}
