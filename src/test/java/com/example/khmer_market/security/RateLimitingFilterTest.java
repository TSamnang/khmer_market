package com.example.khmer_market.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RateLimitingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Exceeding login rate limit (5 attempts/min) triggers HTTP 429 and Retry-After header")
    void testLoginRateLimitExceeded() throws Exception {
        // First 5 attempts from this test IP should be allowed through to auth (returning 302 redirect)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login")
                    .with(csrf())
                    .with(request -> {
                        request.setRemoteAddr("192.168.10.99");
                        return request;
                    })
                    .param("username", "ratelimittest")
                    .param("password", "SomePassword123!"))
                    .andExpect(status().is3xxRedirection());
        }

        // 6th attempt must be rejected with HTTP 429 Too Many Requests
        mockMvc.perform(post("/login")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr("192.168.10.99");
                    return request;
                })
                .param("username", "ratelimittest")
                .param("password", "SomePassword123!"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
