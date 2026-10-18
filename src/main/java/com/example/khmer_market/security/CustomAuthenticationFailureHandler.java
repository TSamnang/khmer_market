package com.example.khmer_market.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public CustomAuthenticationFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String usernameOrEmail = request.getParameter("username");
        boolean isAdmin = request.getRequestURI().contains("/admin");

        Long remainingSeconds = null;
        if (usernameOrEmail != null && !usernameOrEmail.isBlank()) {
            remainingSeconds = loginAttemptService.loginFailed(usernameOrEmail.trim());
            if (remainingSeconds == null) {
                remainingSeconds = loginAttemptService.getRemainingLockSeconds(usernameOrEmail.trim());
            }
        }

        String targetUrl = isAdmin ? "/admin/login?error" : "/login?error";
        if (remainingSeconds != null && remainingSeconds > 0) {
            targetUrl += "&locked=true&lockSeconds=" + remainingSeconds;
        }

        setDefaultFailureUrl(targetUrl);
        super.onAuthenticationFailure(request, response, exception);
    }
}
