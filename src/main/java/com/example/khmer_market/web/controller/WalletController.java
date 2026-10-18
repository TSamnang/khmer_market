package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.entity.WalletTransaction;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.WalletService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;
    private final com.example.khmer_market.service.StoreConfigService storeConfigService;

    public WalletController(WalletService walletService,
                            com.example.khmer_market.service.StoreConfigService storeConfigService) {
        this.walletService = walletService;
        this.storeConfigService = storeConfigService;
    }

    @GetMapping
    public String viewWallet(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "15") int size,
                             Model model) {
        Long userId = userDetails.getUserId();
        User user = walletService.getUser(userId);
        BigDecimal balance = user.getBalance();
        BigDecimal exchangeRate = (storeConfigService != null) ? storeConfigService.getExchangeRateUsdKhr() : new BigDecimal("4100");
        BigDecimal balanceKhr = balance.multiply(exchangeRate).setScale(0, RoundingMode.HALF_UP);

        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        Page<WalletTransaction> transactions = walletService.getTransactionHistory(userId, pageable);

        model.addAttribute("user", user);
        model.addAttribute("balance", balance);
        model.addAttribute("balanceKhr", balanceKhr);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        if (storeConfigService != null) {
            model.addAttribute("config", storeConfigService.getConfig());
        }

        return "buyer/wallet";
    }

    @PostMapping("/deposit")
    public String deposit(@AuthenticationPrincipal CustomUserDetails userDetails,
                          @RequestParam("amount") @NotNull @DecimalMin("0.01") BigDecimal amount,
                          @RequestParam(value = "source", defaultValue = "Instant KHQR") String source,
                          RedirectAttributes redirectAttributes) {
        try {
            WalletTransaction tx = walletService.deposit(userDetails.getUserId(), amount, source, null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully deposited $" + tx.getAmount() + " to your account balance!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/wallet";
    }

    @PostMapping("/withdraw")
    public String withdraw(@AuthenticationPrincipal CustomUserDetails userDetails,
                           @RequestParam("amount") @NotNull @DecimalMin("1.00") BigDecimal amount,
                           @RequestParam("channel") @NotBlank String channel,
                           @RequestParam("accountNumber") @NotBlank String accountNumber,
                           @RequestParam(value = "accountName", required = false) String accountName,
                           RedirectAttributes redirectAttributes) {
        try {
            WalletTransaction tx = walletService.withdraw(userDetails.getUserId(), amount, channel, accountNumber, accountName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Withdrawal of $" + tx.getAmount() + " to " + channel + " (" + accountNumber + ") completed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/wallet";
    }
}
