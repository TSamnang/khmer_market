package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.FarmerProfile;
import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.repository.FarmerProfileRepository;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.UserService;
import com.example.khmer_market.web.dto.BecomeSellerDto;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

@Controller
@RequestMapping("/become-seller")
public class BecomeSellerController {

    private final UserService userService;
    private final FarmerProfileRepository farmerProfileRepository;

    public BecomeSellerController(UserService userService,
                                  FarmerProfileRepository farmerProfileRepository) {
        this.userService = userService;
        this.farmerProfileRepository = farmerProfileRepository;
    }

    @GetMapping
    public String showForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Long userId = userDetails.getUserId();
        User user = userService.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        // If user is already active farmer, redirect to farmer dashboard
        if (user.getRole() == UserRole.FARMER && user.getStatus() == UserStatus.ACTIVE) {
            return "redirect:/farmer/dashboard";
        }

        // If user already applied and is pending or suspended
        var existingProfile = farmerProfileRepository.findByUserId(userId).orElse(null);
        if (existingProfile != null && (user.getStatus() == UserStatus.PENDING || existingProfile.isPending())) {
            return "redirect:/become-seller/status";
        }

        BecomeSellerDto dto = new BecomeSellerDto();
        dto.setFarmerName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        dto.setPhone(user.getPhone() != null ? user.getPhone() : "");

        model.addAttribute("becomeSellerDto", dto);
        model.addAttribute("user", user);
        return "buyer/become-seller";
    }

    @PostMapping
    public String submitVerification(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid @ModelAttribute("becomeSellerDto") BecomeSellerDto dto,
                                     BindingResult bindingResult,
                                     @RequestParam(value = "idFront", required = false) MultipartFile idFront,
                                     @RequestParam(value = "idBack", required = false) MultipartFile idBack,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Long userId = userDetails.getUserId();
        User user = userService.findById(userId).orElse(null);

        // eKYC file presence check
        if (idFront == null || idFront.isEmpty()) {
            bindingResult.reject("required.idFront", "Front photo of your government ID is required for eKYC verification");
        }
        if (idBack == null || idBack.isEmpty()) {
            bindingResult.reject("required.idBack", "Back photo of your government ID or Selfie with ID is required");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "buyer/become-seller";
        }

        try {
            userService.applyToBecomeSeller(userId, dto, idFront, idBack);

            // Dynamically refresh SecurityContext with ROLE_FARMER
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        auth.getPrincipal(),
                        auth.getCredentials(),
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_FARMER"))
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Your Seller Verification (eKYC) has been submitted successfully! It is now pending review by Khmer Market administration.");
            return "redirect:/become-seller/status";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", user);
            return "buyer/become-seller";
        }
    }

    @GetMapping("/status")
    public String viewStatus(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Long userId = userDetails.getUserId();
        User user = userService.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        FarmerProfile profile = farmerProfileRepository.findByUserId(userId).orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        return "buyer/seller-status";
    }
}
