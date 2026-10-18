package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.FarmerProfile;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.FarmerService;
import com.example.khmer_market.web.dto.FarmerProfileDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/farmer/profile")
public class FarmerProfileController {

    private final FarmerService farmerService;

    public FarmerProfileController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        FarmerProfile profile = farmerService.getProfile(userDetails.getUserId());
        FarmerProfileDto dto = new FarmerProfileDto();
        dto.setFarmName(profile.getFarmName());
        dto.setFarmerName(profile.getFarmerName());
        dto.setPhone(profile.getPhone());
        dto.setProvince(profile.getProvince());
        dto.setAddress(profile.getAddress());
        dto.setFarmDescription(profile.getFarmDescription());
        dto.setFarmImage(profile.getFarmImage());

        model.addAttribute("profile", profile);
        model.addAttribute("profileDto", dto);
        return "farmer/profile";
    }

    @PostMapping
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @Valid @ModelAttribute("profileDto") FarmerProfileDto profileDto,
                                BindingResult bindingResult,
                                @RequestParam(value = "farmImageFile", required = false) MultipartFile farmImageFile,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            FarmerProfile profile = farmerService.getProfile(userDetails.getUserId());
            model.addAttribute("profile", profile);
            return "farmer/profile";
        }

        try {
            farmerService.updateProfile(userDetails.getUserId(), profileDto, farmImageFile);
            redirectAttributes.addFlashAttribute("successMessage", "Farm profile updated successfully");
            return "redirect:/farmer/profile";
        } catch (Exception e) {
            FarmerProfile profile = farmerService.getProfile(userDetails.getUserId());
            model.addAttribute("profile", profile);
            model.addAttribute("errorMessage", e.getMessage());
            return "farmer/profile";
        }
    }
}
