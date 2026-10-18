package com.example.khmer_market.security;

import com.example.khmer_market.service.RateLimitService;
import com.example.khmer_market.service.RateLimitService.RateLimitPolicy;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class IpRateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IpRateLimitingFilter.class);

    private final TrustedIpResolver ipResolver;
    private final RateLimitService rateLimitService;

    // Caffeine cache with 1 hour expiry after last access to prevent memory leaks
    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(50_000)
            .build();

    public IpRateLimitingFilter(TrustedIpResolver ipResolver, RateLimitService rateLimitService) {
        this.ipResolver = ipResolver;
        this.rateLimitService = rateLimitService;
    }

    @PostConstruct
    public void registerCacheClearer() {
        this.rateLimitService.setOnPolicyUpdateCallback(this::clearCache);
    }

    public void clearCache() {
        bucketCache.invalidateAll();
        log.info("Rate limiter bucket cache cleared following dynamic policy update.");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/fonts/") ||
               path.startsWith("/vendor/") ||
               path.startsWith("/webjars/") ||
               path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = ipResolver.resolveClientIp(request);
        String userKey = resolveUserOrIpKey(clientIp);

        RateLimitRule rule = resolveRule(path, method, clientIp, userKey, request);
        if (rule != null) {
            Bucket bucket = bucketCache.get(rule.cacheKey(), k -> createBucket(rule.capacity(), rule.refillTokens(), rule.refillPeriodSeconds()));
            if (!bucket.tryConsume(1)) {
                long retryAfterSeconds = Math.max(1, rule.refillPeriodSeconds() / Math.max(1, rule.refillTokens()));
                log.warn("Security Event: Rate limit exceeded for path '{}' by key '{}'. Retry-After: {}s",
                        path, rule.cacheKey(), retryAfterSeconds);

                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

                if (isJsonRequest(request)) {
                    response.setContentType("application/json;charset=UTF-8");
                    String json = String.format("{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Please try again later.\",\"retryAfterSeconds\":%d}}", retryAfterSeconds);
                    response.getWriter().write(json);
                    return;
                } else {
                    request.setAttribute("retryAfterSeconds", retryAfterSeconds);
                    request.getRequestDispatcher("/error/429").forward(request, response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(String path, String method, String clientIp, String userKey, HttpServletRequest request) {
        if ("/admin/login".equals(path) && "POST".equalsIgnoreCase(method)) {
            RateLimitPolicy p = rateLimitService.getPolicy("admin-login");
            return new RateLimitRule("rl:admin_login:" + clientIp, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if ("/login".equals(path) && "POST".equalsIgnoreCase(method)) {
            RateLimitPolicy p = rateLimitService.getPolicy("customer-login");
            return new RateLimitRule("rl:login:" + clientIp, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if ("/register".equals(path) && "POST".equalsIgnoreCase(method)) {
            RateLimitPolicy p = rateLimitService.getPolicy("registration");
            return new RateLimitRule("rl:register:" + clientIp, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if ("/forgot-password".equals(path) && "POST".equalsIgnoreCase(method)) {
            RateLimitPolicy p = rateLimitService.getPolicy("forgot-password");
            String email = request.getParameter("email");
            String key = (email != null && !email.isBlank()) ? "rl:forgot:" + clientIp + ":" + email.trim().toLowerCase() : "rl:forgot:" + clientIp;
            return new RateLimitRule(key, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if (path.startsWith("/buyer/checkout") && "POST".equalsIgnoreCase(method)) {
            RateLimitPolicy p = rateLimitService.getPolicy("checkout");
            return new RateLimitRule("rl:checkout:" + userKey, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if (path.startsWith("/api/payments") || path.startsWith("/api/payment")) {
            RateLimitPolicy p = rateLimitService.getPolicy("payment-webhook");
            return new RateLimitRule("rl:webhook:" + clientIp, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if ((path.startsWith("/api/cart") || path.contains("/favorites/toggle")) && !"GET".equalsIgnoreCase(method)) {
            RateLimitPolicy p = rateLimitService.getPolicy("cart-ajax");
            return new RateLimitRule("rl:cart_fav:" + userKey, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if (path.startsWith("/buyer/reviews") && "POST".equalsIgnoreCase(method)) {
            RateLimitPolicy p = rateLimitService.getPolicy("review-submit");
            return new RateLimitRule("rl:review:" + userKey, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if ((path.contains("/images") || path.contains("/image")) && "POST".equalsIgnoreCase(method)) {
            RateLimitPolicy p = rateLimitService.getPolicy("file-upload");
            return new RateLimitRule("rl:upload:" + userKey, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if (path.startsWith("/admin/") || path.startsWith("/admin") || path.startsWith("/farmer/") || path.startsWith("/buyer/")) {
            RateLimitPolicy p = rateLimitService.getPolicy("portal-routes");
            return new RateLimitRule("rl:portal:" + userKey, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if (path.startsWith("/products") && "GET".equalsIgnoreCase(method)) {
            RateLimitPolicy p = rateLimitService.getPolicy("public-search");
            return new RateLimitRule("rl:search:" + clientIp, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        if ("GET".equalsIgnoreCase(method)) {
            RateLimitPolicy p = rateLimitService.getPolicy("public-get");
            return new RateLimitRule("rl:public_get:" + clientIp, p.getCapacity(), p.getCapacity(), p.getPeriodSeconds());
        }
        return null;
    }

    private String resolveUserOrIpKey(String clientIp) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        return "ip:" + clientIp;
    }

    private Bucket createBucket(long capacity, long refillTokens, long refillPeriodSeconds) {
        Refill refill = Refill.greedy(refillTokens, Duration.ofSeconds(refillPeriodSeconds));
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith) || (accept != null && accept.contains("application/json"));
    }

    private record RateLimitRule(String cacheKey, long capacity, long refillTokens, long refillPeriodSeconds) {}
}
