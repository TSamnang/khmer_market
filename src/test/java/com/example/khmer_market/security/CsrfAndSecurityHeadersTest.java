package com.example.khmer_market.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CsrfAndSecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("State-changing POST request without CSRF token must be rejected with 403")
    void testPostWithoutCsrfTokenRejected() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "testuser")
                .param("password", "TestPassword123!"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Post request with valid CSRF token is accepted by security filter")
    void testPostWithCsrfTokenAccepted() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "nonexistent")
                .param("password", "TestPassword123!"))
                // Expect 302 redirect back to login?error rather than 403 CSRF forbidden
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("Verify security headers: Strict CSP, nosniff, DENY, strict-origin-when-cross-origin")
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }
}
