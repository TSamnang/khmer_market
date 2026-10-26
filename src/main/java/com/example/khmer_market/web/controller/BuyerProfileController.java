package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.FarmerProfile;
import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.entity.UserPaymentMethod;
import com.example.khmer_market.repository.FarmerProfileRepository;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.PaymentMethodService;
import com.example.khmer_market.service.UserService;
import com.example.khmer_market.web.dto.ChangePasswordDto;
import com.example.khmer_market.web.dto.PaymentMethodDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class BuyerProfileController {

    private final UserService userService;
    private final PaymentMethodService paymentMethodService;
    private final FarmerProfileRepository farmerProfileRepository;

    public BuyerProfileController(UserService userService,
                                  PaymentMethodService paymentMethodService,
                                  FarmerProfileRepository farmerProfileRepository) {
        this.userService = userService;
        this.paymentMethodService = paymentMethodService;
        this.farmerProfileRepository = farmerProfileRepository;
    }

    @GetMapping({"/buyer/profile", "/buyer/settings"})
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam(value = "tab", defaultValue = "profile") String tab,
                              Model model) {
        Long userId = userDetails.getUserId();
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<UserPaymentMethod> paymentMethods = paymentMethodService.getUserPaymentMethods(userId);
        FarmerProfile farmerProfile = farmerProfileRepository.findByUserId(userId).orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        model.addAttribute("newPaymentMethod", new PaymentMethodDto());
        model.addAttribute("paymentMethods", paymentMethods);
        model.addAttribute("farmerProfile", farmerProfile);
        model.addAttribute("activeTab", tab);
        return "buyer/profile";
    }

    @PostMapping({"/buyer/profile/change-password", "/buyer/settings/change-password"})
    public String changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @Valid @ModelAttribute("changePasswordDto") ChangePasswordDto changePasswordDto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        Long userId = userDetails.getUserId();
        if (bindingResult.hasErrors()) {
            User user = userService.findById(userId).orElse(null);
            model.addAttribute("user", user);
            model.addAttribute("newPaymentMethod", new PaymentMethodDto());
            model.addAttribute("paymentMethods", paymentMethodService.getUserPaymentMethods(userId));
            model.addAttribute("farmerProfile", farmerProfileRepository.findByUserId(userId).orElse(null));
            model.addAttribute("activeTab", "password");
            return "buyer/profile";
        }

        try {
            userService.changePassword(userId, changePasswordDto);
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully. Please use your new password next time you sign in.");
            return "redirect:/buyer/settings?tab=password";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            User user = userService.findById(userId).orElse(null);
            model.addAttribute("user", user);
            model.addAttribute("newPaymentMethod", new PaymentMethodDto());
            model.addAttribute("paymentMethods", paymentMethodService.getUserPaymentMethods(userId));
            model.addAttribute("farmerProfile", farmerProfileRepository.findByUserId(userId).orElse(null));
            model.addAttribute("activeTab", "password");
            return "buyer/profile";
        }
    }

    @PostMapping({"/buyer/profile/payment-methods", "/buyer/settings/payment-methods"})
    public String addPaymentMethod(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @Valid @ModelAttribute("newPaymentMethod") PaymentMethodDto paymentMethodDto,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        Long userId = userDetails.getUserId();
        if (bindingResult.hasErrors()) {
            User user = userService.findById(userId).orElse(null);
            model.addAttribute("user", user);
            model.addAttribute("changePasswordDto", new ChangePasswordDto());
            model.addAttribute("paymentMethods", paymentMethodService.getUserPaymentMethods(userId));
            model.addAttribute("farmerProfile", farmerProfileRepository.findByUserId(userId).orElse(null));
            model.addAttribute("activeTab", "payment-methods");
            return "buyer/profile";
        }

        try {
            paymentMethodService.savePaymentMethod(userId, paymentMethodDto);
            redirectAttributes.addFlashAttribute("successMessage", "Payment method saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/settings?tab=payment-methods";
    }

    @PostMapping({"/buyer/profile/payment-methods/{id}/default", "/buyer/settings/payment-methods/{id}/default"})
    public String setDefaultPaymentMethod(@PathVariable Long id,
                                          @AuthenticationPrincipal CustomUserDetails userDetails,
                                          RedirectAttributes redirectAttributes) {
        try {
            paymentMethodService.setDefaultPaymentMethod(id, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Default payment method updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/settings?tab=payment-methods";
    }

    @PostMapping({"/buyer/profile/payment-methods/{id}/delete", "/buyer/settings/payment-methods/{id}/delete"})
    public String deletePaymentMethod(@PathVariable Long id,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        try {
            paymentMethodService.deletePaymentMethod(id, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Payment method removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/settings?tab=payment-methods";
    }
}
