package com.example.khmer_market.admin;

import com.example.khmer_market.service.AuditService;
import com.example.khmer_market.service.RateLimitService;
import com.example.khmer_market.service.RateLimitService.RateLimitPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminRateLimitSettingsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private AuditService auditService;

    @Test
    @DisplayName("Admin can view settings page with all rate limiting policies")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testViewSettingsPage() throws Exception {
        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings"))
                .andExpect(model().attributeExists("primaryPolicies"))
                .andExpect(model().attributeExists("additionalPolicies"))
                .andExpect(model().attributeExists("allPolicies"));
    }

    @Test
    @DisplayName("Admin can update rate limit policy dynamically and trigger audit logging")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testUpdateRateLimitPolicy() throws Exception {
        // Update customer-login to 15 requests per 120s
        mockMvc.perform(post("/admin/settings/rate-limit")
                        .with(csrf())
                        .param("policyId", "customer-login")
                        .param("capacity", "15")
                        .param("periodSeconds", "120"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"))
                .andExpect(flash().attributeExists("successMessage"));

        RateLimitPolicy updated = rateLimitService.getPolicy("customer-login");
        assertEquals(15, updated.getCapacity());
        assertEquals(120, updated.getPeriodSeconds());

        // Verify audit log has the event
        boolean hasAudit = auditService.getRecentEvents().stream()
                .anyMatch(e -> "UPDATE_RATE_LIMIT".equals(e.action()) && "customer-login".equals(e.targetEntityId()));
        assertTrue(hasAudit, "Audit event for rate limit update should be recorded");

        // Clean up: reset to default
        rateLimitService.resetToDefaults("admin", "127.0.0.1");
        RateLimitPolicy reset = rateLimitService.getPolicy("customer-login");
        assertEquals(reset.getDefaultCapacity(), reset.getCapacity());
        assertEquals(reset.getDefaultPeriodSeconds(), reset.getPeriodSeconds());
    }

    @Test
    @DisplayName("Admin can reset all rate limits to default values")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testResetRateLimits() throws Exception {
        // First modify a policy
        rateLimitService.updatePolicy("checkout", 25, 300, "admin", "127.0.0.1");
        assertEquals(25, rateLimitService.getPolicy("checkout").getCapacity());

        // Call reset endpoint
        mockMvc.perform(post("/admin/settings/rate-limit/reset")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"))
                .andExpect(flash().attributeExists("successMessage"));

        RateLimitPolicy resetPolicy = rateLimitService.getPolicy("checkout");
        assertEquals(resetPolicy.getDefaultCapacity(), resetPolicy.getCapacity());
    }

    @Test
    @DisplayName("Non-admin user is rejected from updating rate limit policies")
    @WithMockUser(username = "buyer1", roles = {"BUYER"})
    void testNonAdminCannotUpdateRateLimit() throws Exception {
        mockMvc.perform(post("/admin/settings/rate-limit")
                        .with(csrf())
                        .param("policyId", "customer-login")
                        .param("capacity", "100")
                        .param("periodSeconds", "60"))
                .andExpect(status().isForbidden());
    }
}
