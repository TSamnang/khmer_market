package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.repository.OrderRepository;
import com.example.khmer_market.service.AuditService;
import com.example.khmer_market.service.EscrowService;
import com.example.khmer_market.service.StoreConfigService;
import com.example.khmer_market.web.dto.StoreConfigDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/bank")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBankController {

    private final StoreConfigService storeConfigService;
    private final EscrowService escrowService;
    private final OrderRepository orderRepository;
    private final AuditService auditService;

    public AdminBankController(StoreConfigService storeConfigService,
                               EscrowService escrowService,
                               OrderRepository orderRepository,
                               AuditService auditService) {
        this.storeConfigService = storeConfigService;
        this.escrowService = escrowService;
        this.orderRepository = orderRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public String bankPage(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "15") int size,
                           Model model) {
        Page<Order> escrowOrders = escrowService.getEscrowOrders(PageRequest.of(Math.max(0, page), size));

        model.addAttribute("config", storeConfigService.getConfig());
        model.addAttribute("storeConfigDto", StoreConfigDto.fromConfig(storeConfigService.getConfig()));
        model.addAttribute("escrowSummary", escrowService.getEscrowSummary());
        model.addAttribute("escrowOrders", escrowOrders);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", escrowOrders.getTotalPages());
        model.addAttribute("activeNav", "bank");
        return "admin/bank";
    }

    @PostMapping("/update")
    public String updateBankConfig(@Valid @ModelAttribute("storeConfigDto") StoreConfigDto dto,
                                   BindingResult bindingResult,
                                   Principal principal,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (bindingResult.hasFieldErrors("bankName") ||
            bindingResult.hasFieldErrors("bankAccountNumber") ||
            bindingResult.hasFieldErrors("bankAccountName")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bank Name, Account Number, and Account Name are required.");
            return "redirect:/admin/bank";
        }

        String username = (principal != null) ? principal.getName() : "admin";
        storeConfigService.updateBankConfig(dto, username, request.getRemoteAddr());

        redirectAttributes.addFlashAttribute("successMessage",
                "Platform Master Bank & Gateway API credentials successfully updated.");
        return "redirect:/admin/bank";
    }

    @PostMapping("/release/{id}")
    public String manualReleaseEscrow(@PathVariable Long id,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Order not found: " + id);
            return "redirect:/admin/bank";
        }

        if (order.isEscrowReleased()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Escrow funds for order " + order.getOrderNumber() + " were already released.");
            return "redirect:/admin/bank";
        }

        boolean released = escrowService.releaseEscrow(order, "Admin Manual Early Release by " + (principal != null ? principal.getName() : "admin"));
        if (released) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Escrow funds for Order #" + order.getOrderNumber() + " released to seller balance. Platform commission retained in Master Bank.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Could not release escrow for Order #" + order.getOrderNumber() + ". Verify payment status.");
        }

        return "redirect:/admin/bank";
    }

    @PostMapping("/auto-confirm")
    public String triggerAutoConfirm(RedirectAttributes redirectAttributes) {
        int count = escrowService.autoConfirmExpiredEscrows();
        redirectAttributes.addFlashAttribute("successMessage",
                "7-Day Escrow Check Completed: " + count + " expired order(s) auto-confirmed and released to seller balances.");
        return "redirect:/admin/bank";
    }
}
