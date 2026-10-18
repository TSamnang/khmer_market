package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Product;
import com.example.khmer_market.domain.enums.ProductStatus;
import com.example.khmer_market.repository.CategoryRepository;
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
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminService adminService;
    private final CategoryRepository categoryRepository;

    public AdminProductController(AdminService adminService, CategoryRepository categoryRepository) {
        this.adminService = adminService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String listProducts(@RequestParam(required = false) String query,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false) Long sellerId,
                               @RequestParam(required = false) ProductStatus status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "15") int size,
                               Model model) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        Page<Product> products = adminService.searchProducts(query, categoryId, sellerId, status, pageable);

        model.addAttribute("products", products);
        model.addAttribute("query", query);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalElements", products.getTotalElements());
        model.addAttribute("activeNav", "products");
        return "admin/products";
    }

    @GetMapping("/{id}")
    public String viewProduct(@PathVariable Long id, Model model) {
        Product product = adminService.getProduct(id);
        model.addAttribute("product", product);
        model.addAttribute("activeNav", "products");
        return "admin/product-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam ProductStatus status,
                               java.security.Principal principal,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.toggleProductStatus(id, status, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Product status updated to " + status.name());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + id;
    }
}
