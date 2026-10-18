package com.example.khmer_market.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class AdminIpFilter extends OncePerRequestFilter {

    private final TrustedIpResolver ipResolver;

    @Value("${market.security.admin-allowed-ips:}")
    private String allowedIpsConfig;

    public AdminIpFilter(TrustedIpResolver ipResolver) {
        this.ipResolver = ipResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (allowedIpsConfig != null && !allowedIpsConfig.isBlank()) {
            List<String> allowed = Arrays.stream(allowedIpsConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            if (!allowed.isEmpty()) {
                String clientIp = ipResolver.resolveClientIp(request);
                if (!allowed.contains(clientIp) && !allowed.contains("127.0.0.1") && !allowed.contains("0:0:0:0:0:0:0:1")) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access restricted by IP policy");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
