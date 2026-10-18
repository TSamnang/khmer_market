package com.example.khmer_market.web.controller;

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
@RequestMapping("/admin/store-customize")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreCustomizeController {

    private final StoreConfigService storeConfigService;

    public AdminStoreCustomizeController(StoreConfigService storeConfigService) {
        this.storeConfigService = storeConfigService;
    }

    @GetMapping
    public String viewStoreCustomize(Model model) {
        if (!model.containsAttribute("storeConfigDto")) {
            model.addAttribute("storeConfigDto", StoreConfigDto.fromConfig(storeConfigService.getConfig()));
        }
        model.addAttribute("activeNav", "store-customize");
        return "admin/store-customize";
    }

    @PostMapping
    public String updateStoreConfig(@Valid @ModelAttribute("storeConfigDto") StoreConfigDto dto,
                                    BindingResult bindingResult,
                                    Principal principal,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeNav", "store-customize");
            return "admin/store-customize";
        }

        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            storeConfigService.updateConfig(dto, adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Storefront customization and brand settings saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update store settings: " + e.getMessage());
        }

        return "redirect:/admin/store-customize";
    }

    @PostMapping("/reset")
    public String resetToDefaults(Principal principal,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            storeConfigService.resetToDefaults(adminUsername, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "All storefront and business settings restored to factory defaults.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reset settings: " + e.getMessage());
        }

        return "redirect:/admin/store-customize";
    }
}
