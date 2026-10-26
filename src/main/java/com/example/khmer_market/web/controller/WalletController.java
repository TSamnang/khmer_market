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
    private final com.example.khmer_market.service.PaymentMethodService paymentMethodService;

    @org.springframework.beans.factory.annotation.Autowired
    public WalletController(WalletService walletService,
                            com.example.khmer_market.service.StoreConfigService storeConfigService,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) com.example.khmer_market.service.PaymentMethodService paymentMethodService) {
        this.walletService = walletService;
        this.storeConfigService = storeConfigService;
        this.paymentMethodService = paymentMethodService;
    }

    public WalletController(WalletService walletService,
                            com.example.khmer_market.service.StoreConfigService storeConfigService) {
        this(walletService, storeConfigService, null);
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
        if (paymentMethodService != null) {
            model.addAttribute("userPaymentMethods", paymentMethodService.getUserPaymentMethods(userId));
        } else {
            model.addAttribute("userPaymentMethods", java.util.Collections.emptyList());
        }

        return "buyer/wallet";
    }

    @PostMapping("/deposit")
    public String deposit(@AuthenticationPrincipal CustomUserDetails userDetails,
                          @RequestParam("amount") @NotNull @DecimalMin("0.01") BigDecimal amount,
                          @RequestParam(value = "paymentMethodId", required = false) Long paymentMethodId,
                          @RequestParam(value = "source", required = false, defaultValue = "Credit / Debit Card") String source,
                          @RequestParam(value = "saveMethod", defaultValue = "false") boolean saveMethod,
                          @RequestParam(value = "customProvider", required = false) String customProvider,
                          @RequestParam(value = "customAccountNumber", required = false) String customAccountNumber,
                          @RequestParam(value = "customAccountHolderName", required = false) String customAccountHolderName,
                          RedirectAttributes redirectAttributes) {
        try {
            Long userId = userDetails.getUserId();
            String effectiveSource = source;

            if (paymentMethodId != null && paymentMethodService != null) {
                var method = paymentMethodService.getPaymentMethod(paymentMethodId, userId);
                effectiveSource = method.getProvider() + " (" + method.getMaskedAccountNumber() + ")";
            } else if (saveMethod && customAccountNumber != null && !customAccountNumber.isBlank() && paymentMethodService != null) {
                var dto = new com.example.khmer_market.web.dto.PaymentMethodDto();
                dto.setMethodType(customProvider != null && (customProvider.contains("Card") || customProvider.contains("Visa")) ? "CARD" : "BANK_ACCOUNT");
                dto.setProvider(customProvider != null && !customProvider.isBlank() ? customProvider : "ABA Bank");
                dto.setAccountNumber(customAccountNumber.trim());
                dto.setAccountHolderName(customAccountHolderName != null && !customAccountHolderName.isBlank() ? customAccountHolderName.trim() : userDetails.getFullName());
                dto.setDefault(false);
                var saved = paymentMethodService.savePaymentMethod(userId, dto);
                effectiveSource = saved.getProvider() + " (" + saved.getMaskedAccountNumber() + ")";
            }

            WalletTransaction tx = walletService.deposit(userId, amount, effectiveSource, null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully deposited $" + tx.getAmount() + " from " + effectiveSource + " to your personal wallet!");
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
