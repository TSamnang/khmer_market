package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.UserService;
import com.example.khmer_market.web.dto.ChangePasswordDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/buyer/profile")
public class BuyerProfileController {

    private final UserService userService;

    public BuyerProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userService.findById(userDetails.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        model.addAttribute("user", user);
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        return "buyer/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @Valid @ModelAttribute("changePasswordDto") ChangePasswordDto changePasswordDto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            User user = userService.findById(userDetails.getUserId()).orElse(null);
            model.addAttribute("user", user);
            return "buyer/profile";
        }

        try {
            userService.changePassword(userDetails.getUserId(), changePasswordDto);
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully. Please use your new password next time you sign in.");
            return "redirect:/buyer/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            User user = userService.findById(userDetails.getUserId()).orElse(null);
            model.addAttribute("user", user);
            return "buyer/profile";
        }
    }
}
