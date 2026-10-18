package com.example.khmer_market.web.controller;

import com.example.khmer_market.service.PasswordResetService;
import com.example.khmer_market.service.UserService;
import com.example.khmer_market.web.dto.ForgotPasswordDto;
import com.example.khmer_market.web.dto.RegisterDto;
import com.example.khmer_market.web.dto.ResetPasswordDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public AuthController(UserService userService,
                          PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            @RequestParam(value = "resetSuccess", required = false) String resetSuccess,
                            @RequestParam(value = "locked", required = false) String locked,
                            @RequestParam(value = "lockSeconds", required = false) Long lockSeconds,
                            Model model) {

        if (error != null) {
            model.addAttribute("loginError", true);
        }
        if (logout != null) {
            model.addAttribute("logoutSuccess", true);
        }
        if (registered != null) {
            model.addAttribute("registeredSuccess", true);
        }
        if (resetSuccess != null) {
            model.addAttribute("resetSuccess", true);
        }
        if (locked != null) {
            model.addAttribute("isAccountLocked", true);
            model.addAttribute("remainingSeconds", lockSeconds != null ? lockSeconds : 900);
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerDto") RegisterDto registerDto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        if (!registerDto.isPasswordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "error.passwordMatch", "Passwords do not match / ពាក្យសម្ងាត់មិនត្រូវគ្នាទេ");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(registerDto);
            redirectAttributes.addFlashAttribute("registrationSuccess", true);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        if (!model.containsAttribute("forgotPasswordDto")) {
            model.addAttribute("forgotPasswordDto", new ForgotPasswordDto());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@Valid @ModelAttribute("forgotPasswordDto") ForgotPasswordDto forgotPasswordDto,
                                       BindingResult bindingResult,
                                       HttpServletRequest request,
                                       Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        String baseUrl = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }

        passwordResetService.createPasswordResetToken(forgotPasswordDto.getEmail().trim(), baseUrl);
        model.addAttribute("emailSent", true);
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("invalidToken", true);
            return "auth/reset-password";
        }

        ResetPasswordDto dto = new ResetPasswordDto();
        dto.setToken(token);
        model.addAttribute("resetPasswordDto", dto);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@Valid @ModelAttribute("resetPasswordDto") ResetPasswordDto dto,
                                      BindingResult bindingResult,
                                      Model model) {

        if (!dto.isPasswordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "error.passwordMatch", "Passwords do not match / ពាក្យសម្ងាត់មិនត្រូវគ្នាទេ");
        }

        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            boolean success = passwordResetService.resetPassword(dto.getToken(), dto.getPassword());
            if (!success) {
                model.addAttribute("invalidToken", true);
                return "auth/reset-password";
            }
            return "redirect:/login?resetSuccess";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/reset-password";
        }
    }
}
