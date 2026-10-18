package com.example.khmer_market.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final AuditService auditService;
    private final Map<String, RateLimitPolicy> policies = new ConcurrentHashMap<>();
    private final List<String> primaryPolicyOrder = new ArrayList<>();
    private final List<String> additionalPolicyOrder = new ArrayList<>();
    private Runnable onPolicyUpdateCallback;

    // Configurable defaults injected from application.yml
    @Value("${market.rate-limit.login.capacity:5}")
    private long defaultLoginCapacity;
    @Value("${market.rate-limit.login.refill-period-seconds:60}")
    private long defaultLoginPeriod;

    @Value("${market.rate-limit.admin-login.capacity:5}")
    private long defaultAdminLoginCapacity;
    @Value("${market.rate-limit.admin-login.refill-period-seconds:60}")
    private long defaultAdminLoginPeriod;

    @Value("${market.rate-limit.register.capacity:5}")
    private long defaultRegisterCapacity;
    @Value("${market.rate-limit.register.refill-period-seconds:3600}")
    private long defaultRegisterPeriod;

    @Value("${market.rate-limit.checkout.capacity:10}")
    private long defaultCheckoutCapacity;
    @Value("${market.rate-limit.checkout.refill-period-seconds:60}")
    private long defaultCheckoutPeriod;

    @Value("${market.rate-limit.public-search.capacity:100}")
    private long defaultSearchCapacity;
    @Value("${market.rate-limit.public-search.refill-period-seconds:60}")
    private long defaultSearchPeriod;

    @Value("${market.rate-limit.admin-console.capacity:120}")
    private long defaultPortalCapacity;
    @Value("${market.rate-limit.admin-console.refill-period-seconds:60}")
    private long defaultPortalPeriod;

    @Value("${market.rate-limit.webhook.capacity:60}")
    private long defaultWebhookCapacity;
    @Value("${market.rate-limit.webhook.refill-period-seconds:60}")
    private long defaultWebhookPeriod;

    @Value("${market.rate-limit.forgot-password.capacity:3}")
    private long defaultForgotCapacity;
    @Value("${market.rate-limit.forgot-password.refill-period-seconds:3600}")
    private long defaultForgotPeriod;

    @Value("${market.rate-limit.cart-ajax.capacity:60}")
    private long defaultCartAjaxCapacity;
    @Value("${market.rate-limit.cart-ajax.refill-period-seconds:60}")
    private long defaultCartAjaxPeriod;

    @Value("${market.rate-limit.review-submit.capacity:10}")
    private long defaultReviewCapacity;
    @Value("${market.rate-limit.review-submit.refill-period-seconds:3600}")
    private long defaultReviewPeriod;

    @Value("${market.rate-limit.file-upload.capacity:20}")
    private long defaultUploadCapacity;
    @Value("${market.rate-limit.file-upload.refill-period-seconds:3600}")
    private long defaultUploadPeriod;

    @Value("${market.rate-limit.public-get.capacity:300}")
    private long defaultPublicGetCapacity;
    @Value("${market.rate-limit.public-get.refill-period-seconds:60}")
    private long defaultPublicGetPeriod;

    public RateLimitService(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        initPolicies();
    }

    private void initPolicies() {
        policies.clear();
        primaryPolicyOrder.clear();
        additionalPolicyOrder.clear();

        // 1. The 7 Primary Policies displayed in the main management table
        addPrimaryPolicy("customer-login", "Customer Login (/login)", "/login", "Customer authentication and sign-in protection", defaultLoginCapacity, defaultLoginPeriod);
        addPrimaryPolicy("admin-login", "Admin Login (/admin/login)", "/admin/login", "Administrative login interface protection", defaultAdminLoginCapacity, defaultAdminLoginPeriod);
        addPrimaryPolicy("registration", "Registration (/register)", "/register", "New buyer and farmer account registration", defaultRegisterCapacity, defaultRegisterPeriod);
        addPrimaryPolicy("checkout", "Checkout Processing (/checkout)", "/checkout", "Order placement and payment authorization", defaultCheckoutCapacity, defaultCheckoutPeriod);
        addPrimaryPolicy("public-search", "Public Catalog Search (/products)", "/products", "Marketplace catalog browsing and keyword queries", defaultSearchCapacity, defaultSearchPeriod);
        addPrimaryPolicy("portal-routes", "Authenticated Portal Routes", "/admin/**, /farmer/**, /buyer/**", "Authenticated user portal navigation and operations", defaultPortalCapacity, defaultPortalPeriod);
        addPrimaryPolicy("payment-webhook", "Payment Webhook (/api/payments/**)", "/api/payments/**", "Real-time payment gateway (Bakong KHQR) callbacks", defaultWebhookCapacity, defaultWebhookPeriod);

        // 2. Supporting Protective Security Policies
        addAdditionalPolicy("forgot-password", "Password Reset (/forgot-password)", "/forgot-password", "Self-service password recovery requests", defaultForgotCapacity, defaultForgotPeriod);
        addAdditionalPolicy("cart-ajax", "Cart & Favorite AJAX (/api/cart/**)", "/api/cart/**, /favorites/**", "Interactive shopping cart updates and wishlists", defaultCartAjaxCapacity, defaultCartAjaxPeriod);
        addAdditionalPolicy("review-submit", "Review Submission (/buyer/reviews)", "/buyer/reviews", "Product ratings and customer feedback submissions", defaultReviewCapacity, defaultReviewPeriod);
        addAdditionalPolicy("file-upload", "File Uploads (/images/**)", "/images/**", "Farmer profile images and product gallery uploads", defaultUploadCapacity, defaultUploadPeriod);
        addAdditionalPolicy("public-get", "Public Anonymous GET", "GET /*", "General unauthenticated resource requests", defaultPublicGetCapacity, defaultPublicGetPeriod);
    }

    private void addPrimaryPolicy(String id, String name, String route, String description, long capacity, long periodSeconds) {
        RateLimitPolicy policy = new RateLimitPolicy(id, name, route, description, capacity, periodSeconds);
        policies.put(id, policy);
        primaryPolicyOrder.add(id);
    }

    private void addAdditionalPolicy(String id, String name, String route, String description, long capacity, long periodSeconds) {
        RateLimitPolicy policy = new RateLimitPolicy(id, name, route, description, capacity, periodSeconds);
        policies.put(id, policy);
        additionalPolicyOrder.add(id);
    }

    public List<RateLimitPolicy> getPrimaryPolicies() {
        return primaryPolicyOrder.stream().map(policies::get).filter(Objects::nonNull).toList();
    }

    public List<RateLimitPolicy> getAdditionalPolicies() {
        return additionalPolicyOrder.stream().map(policies::get).filter(Objects::nonNull).toList();
    }

    public List<RateLimitPolicy> getAllPolicies() {
        List<RateLimitPolicy> all = new ArrayList<>();
        all.addAll(getPrimaryPolicies());
        all.addAll(getAdditionalPolicies());
        return all;
    }

    public RateLimitPolicy getPolicy(String id) {
        RateLimitPolicy policy = policies.get(id);
        if (policy == null) {
            log.warn("Rate limit policy requested for unknown id: '{}'. Using fallback.", id);
            return new RateLimitPolicy(id, id, id, id, 60, 60);
        }
        return policy;
    }

    public synchronized RateLimitPolicy updatePolicy(String id, long capacity, long periodSeconds, String adminUsername, String clientIp) {
        RateLimitPolicy policy = policies.get(id);
        if (policy == null) {
            throw new IllegalArgumentException("Unknown rate limit policy: " + id);
        }
        if (capacity < 1 || capacity > 100_000) {
            throw new IllegalArgumentException("Allowed requests (capacity) must be between 1 and 100,000.");
        }
        if (periodSeconds < 1 || periodSeconds > 86_400) {
            throw new IllegalArgumentException("Time window (seconds) must be between 1 and 86,400 seconds.");
        }

        long oldCapacity = policy.getCapacity();
        long oldPeriod = policy.getPeriodSeconds();

        policy.setCapacity(capacity);
        policy.setPeriodSeconds(periodSeconds);

        if (onPolicyUpdateCallback != null) {
            try {
                onPolicyUpdateCallback.run();
            } catch (Exception e) {
                log.error("Failed to run cache invalidation callback after updating rate limit: {}", e.getMessage());
            }
        }

        auditService.logAction(
                adminUsername,
                "UPDATE_RATE_LIMIT",
                "RATE_LIMIT_POLICY",
                id,
                clientIp,
                Map.of(
                        "policyName", policy.getName(),
                        "previousLimit", oldCapacity + " req / " + oldPeriod + "s",
                        "newLimit", capacity + " req / " + periodSeconds + "s"
                )
        );

        log.info("Security Audit: Admin '{}' updated rate limit policy '{}' from {}/{}s to {}/{}s",
                adminUsername, policy.getName(), oldCapacity, oldPeriod, capacity, periodSeconds);

        return policy;
    }

    public synchronized void resetToDefaults(String adminUsername, String clientIp) {
        for (RateLimitPolicy policy : policies.values()) {
            policy.setCapacity(policy.getDefaultCapacity());
            policy.setPeriodSeconds(policy.getDefaultPeriodSeconds());
        }

        if (onPolicyUpdateCallback != null) {
            try {
                onPolicyUpdateCallback.run();
            } catch (Exception e) {
                log.error("Failed to run cache invalidation callback after resetting rate limits: {}", e.getMessage());
            }
        }

        auditService.logAction(
                adminUsername,
                "RESET_RATE_LIMITS",
                "RATE_LIMIT_POLICY",
                "ALL",
                clientIp,
                Map.of("action", "Reset all rate limiting policies back to factory defaults")
        );

        log.info("Security Audit: Admin '{}' reset all rate limiting policies to factory defaults", adminUsername);
    }

    public void setOnPolicyUpdateCallback(Runnable callback) {
        this.onPolicyUpdateCallback = callback;
    }

    public static class RateLimitPolicy {
        private final String id;
        private final String name;
        private final String route;
        private final String description;
        private long capacity;
        private long periodSeconds;
        private final long defaultCapacity;
        private final long defaultPeriodSeconds;

        public RateLimitPolicy(String id, String name, String route, String description, long capacity, long periodSeconds) {
            this.id = id;
            this.name = name;
            this.route = route;
            this.description = description;
            this.capacity = capacity;
            this.periodSeconds = periodSeconds;
            this.defaultCapacity = capacity;
            this.defaultPeriodSeconds = periodSeconds;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getRoute() { return route; }
        public String getDescription() { return description; }

        public long getCapacity() { return capacity; }
        public void setCapacity(long capacity) { this.capacity = capacity; }

        public long getPeriodSeconds() { return periodSeconds; }
        public void setPeriodSeconds(long periodSeconds) { this.periodSeconds = periodSeconds; }

        public long getDefaultCapacity() { return defaultCapacity; }
        public long getDefaultPeriodSeconds() { return defaultPeriodSeconds; }

        public String getLimitFormatted() {
            return capacity + " requests per " + periodSeconds + "s";
        }
    }
}
