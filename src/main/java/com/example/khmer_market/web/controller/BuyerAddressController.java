package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Address;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.AddressService;
import com.example.khmer_market.web.dto.AddressDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/buyer/addresses")
public class BuyerAddressController {

    private final AddressService addressService;

    public BuyerAddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public String listAddresses(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Address> addresses = addressService.getUserAddresses(userDetails.getUserId());
        model.addAttribute("addresses", addresses);
        model.addAttribute("addressDto", new AddressDto());
        return "buyer/addresses";
    }

    @PostMapping
    public String addAddress(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @Valid @ModelAttribute("addressDto") AddressDto addressDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            List<Address> addresses = addressService.getUserAddresses(userDetails.getUserId());
            model.addAttribute("addresses", addresses);
            return "buyer/addresses";
        }

        try {
            addressService.createAddress(userDetails.getUserId(), addressDto);
            redirectAttributes.addFlashAttribute("successMessage", "Address added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/addresses";
    }

    @PostMapping("/{id}/delete")
    public String deleteAddress(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            addressService.deleteAddress(id, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Address deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/addresses";
    }
}
