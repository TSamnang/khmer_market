package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Address;
import com.example.khmer_market.domain.entity.FarmerProfile;
import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.repository.AddressRepository;
import com.example.khmer_market.repository.FarmerProfileRepository;
import com.example.khmer_market.repository.OrderRepository;
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
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminService adminService;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final FarmerProfileRepository farmerProfileRepository;

    public AdminUserController(AdminService adminService,
                               OrderRepository orderRepository,
                               AddressRepository addressRepository,
                               FarmerProfileRepository farmerProfileRepository) {
        this.adminService = adminService;
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.farmerProfileRepository = farmerProfileRepository;
    }

    @GetMapping
    public String listUsers(@RequestParam(required = false) String query,
                            @RequestParam(required = false) UserRole role,
                            @RequestParam(required = false) UserStatus status,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            Model model) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        Page<User> users = adminService.searchUsers(query, role, status, pageable);

        model.addAttribute("users", users);
        model.addAttribute("query", query);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("totalElements", users.getTotalElements());
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("activeNav", "users");
        return "admin/users";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = adminService.getUser(id);
        List<Order> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(id, PageRequest.of(0, 10)).getContent();
        List<Address> addresses = addressRepository.findByUserId(id);
        FarmerProfile farmerProfile = user.getRole() == UserRole.FARMER
                ? farmerProfileRepository.findByUserId(id).orElse(null)
                : null;

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("addresses", addresses);
        model.addAttribute("farmerProfile", farmerProfile);
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("activeNav", "users");
        return "admin/user-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam UserStatus status,
                               java.security.Principal principal,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.updateUserStatus(id, status, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "User status updated to " + status.name());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/unlock")
    public String unlockUser(@PathVariable Long id,
                             java.security.Principal principal,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            adminService.unlockUserAccount(id, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Account successfully unlocked and failed attempts reset.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }
}
