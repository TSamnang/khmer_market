package com.example.khmer_market.security;

import com.example.khmer_market.domain.enums.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public CustomAuthenticationSuccessHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        loginAttemptService.loginSucceeded(authentication.getName());

        boolean isAdminLogin = request.getRequestURI().contains("/admin");
        boolean hasAdminRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        // If a non-admin logs in through /admin/login, reject with generic failure and kill session
        if (isAdminLogin && !hasAdminRole) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect("/admin/login?error");
            return;
        }

        if (isAdminLogin || hasAdminRole) {
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, "/admin/dashboard");
            return;
        }

        boolean isFarmer = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_FARMER"));

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            if (isValidRedirect(targetUrl, request)) {
                clearAuthenticationAttributes(request);
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
        }

        clearAuthenticationAttributes(request);
        if (isFarmer) {
            getRedirectStrategy().sendRedirect(request, response, "/farmer/dashboard");
        } else {
            getRedirectStrategy().sendRedirect(request, response, "/");
        }
    }

    private boolean isValidRedirect(String targetUrl, HttpServletRequest request) {
        if (targetUrl == null || targetUrl.isBlank()) return false;

        // Disallow static assets, images, and scripts from being post-login redirect destinations
        String cleanUrl = targetUrl;
        int queryIdx = cleanUrl.indexOf('?');
        if (queryIdx != -1) {
            cleanUrl = cleanUrl.substring(0, queryIdx);
        }
        int hashIdx = cleanUrl.indexOf('#');
        if (hashIdx != -1) {
            cleanUrl = cleanUrl.substring(0, hashIdx);
        }
        String lowerUrl = cleanUrl.toLowerCase();
        if (lowerUrl.endsWith(".svg") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".jpg")
                || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".gif") || lowerUrl.endsWith(".webp")
                || lowerUrl.endsWith(".ico") || lowerUrl.endsWith(".css") || lowerUrl.endsWith(".js")
                || lowerUrl.endsWith(".map") || lowerUrl.contains("/images/") || lowerUrl.contains("/img/")
                || lowerUrl.contains("/static/") || lowerUrl.contains("/css/") || lowerUrl.contains("/js/")) {
            return false;
        }

        // Open redirect protection: relative same-site paths only
        if (targetUrl.startsWith("/") && !targetUrl.startsWith("//") && !targetUrl.startsWith("/\\")) {
            return !targetUrl.contains("/admin");
        }
        String serverName = request.getServerName();
        return targetUrl.contains("://" + serverName) && !targetUrl.contains("/admin");
    }
}
