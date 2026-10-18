package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Category;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.AdminService;
import com.example.khmer_market.web.dto.CategoryFormDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final AdminService adminService;

    public AdminCategoryController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = adminService.getAllCategories();
        Map<Long, Long> productCounts = new HashMap<>();
        for (Category cat : categories) {
            productCounts.put(cat.getId(), adminService.getProductCountForCategory(cat.getId()));
        }

        model.addAttribute("categories", categories);
        model.addAttribute("productCounts", productCounts);
        model.addAttribute("categoryForm", new CategoryFormDto());
        model.addAttribute("activeNav", "categories");
        return "admin/categories";
    }

    @PostMapping
    public String createCategory(@Valid @ModelAttribute("categoryForm") CategoryFormDto form,
                                 BindingResult bindingResult,
                                 @RequestParam(value = "image", required = false) MultipartFile image,
                                 java.security.Principal principal,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            List<Category> categories = adminService.getAllCategories();
            Map<Long, Long> productCounts = new HashMap<>();
            for (Category cat : categories) {
                productCounts.put(cat.getId(), adminService.getProductCountForCategory(cat.getId()));
            }
            model.addAttribute("categories", categories);
            model.addAttribute("productCounts", productCounts);
            model.addAttribute("activeNav", "categories");
            return "admin/categories";
        }

        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.createCategory(form, image, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        Category category = adminService.getCategory(id);
        CategoryFormDto form = new CategoryFormDto(category.getId(), category.getName(), category.getDescription(), category.getImageUrl());
        model.addAttribute("category", category);
        model.addAttribute("categoryForm", form);
        model.addAttribute("activeNav", "categories");
        return "admin/category-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateCategory(@PathVariable Long id,
                                 @Valid @ModelAttribute("categoryForm") CategoryFormDto form,
                                 BindingResult bindingResult,
                                 @RequestParam(value = "image", required = false) MultipartFile image,
                                 java.security.Principal principal,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", adminService.getCategory(id));
            model.addAttribute("activeNav", "categories");
            return "admin/category-edit";
        }

        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.updateCategory(id, form, image, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/categories/" + id + "/edit";
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id,
                                 java.security.Principal principal,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.deleteCategory(id, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
