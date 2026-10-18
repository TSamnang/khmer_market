package com.example.khmer_market.admin;

import com.example.khmer_market.domain.model.StoreConfig;
import com.example.khmer_market.service.StoreConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminStoreCustomizeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreConfigService storeConfigService;

    @Test
    @DisplayName("Admin dashboard displays store customization card and quick customize modal")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminDashboardDisplaysStoreCustomize() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("storeConfig"))
                .andExpect(model().attributeExists("storeConfigDto"));
    }

    @Test
    @DisplayName("Admin can quick customize store from dashboard")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDashboardQuickCustomize() throws Exception {
        mockMvc.perform(post("/admin/dashboard/quick-customize")
                        .with(csrf())
                        .param("storeName", "Angkor Organic Mart")
                        .param("announcementEnabled", "true")
                        .param("announcementText", "Harvest Festival Flash Sale!")
                        .param("announcementBgClass", "bg-warning text-dark")
                        .param("exchangeRateUsdKhr", "4150.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));

        StoreConfig config = storeConfigService.getConfig();
        assertEquals("Angkor Organic Mart", config.getStoreName());
        assertTrue(config.isAnnouncementEnabled());
        assertEquals("Harvest Festival Flash Sale!", config.getAnnouncementText());
        assertEquals("bg-warning text-dark", config.getAnnouncementBgClass());
        assertEquals(0, new BigDecimal("4150.00").compareTo(config.getExchangeRateUsdKhr()));
    }

    @Test
    @DisplayName("Admin can view full store customization page")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testViewStoreCustomizePage() throws Exception {
        mockMvc.perform(get("/admin/store-customize"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/store-customize"))
                .andExpect(model().attributeExists("storeConfigDto"))
                .andExpect(model().attribute("activeNav", "store-customize"));
    }

    @Test
    @DisplayName("Admin can update full store customization configuration")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testUpdateStoreCustomize() throws Exception {
        mockMvc.perform(post("/admin/store-customize")
                        .with(csrf())
                        .param("storeName", "Battambang Fresh Direct")
                        .param("tagline", "Finest Rice & Organic Vegetables")
                        .param("brandColor", "#1e40af")
                        .param("announcementEnabled", "true")
                        .param("announcementText", "Free Delivery on orders above $20 across Battambang!")
                        .param("announcementBgClass", "bg-primary")
                        .param("heroBadgeText", "Fresh Farm Harvest")
                        .param("heroTitle", "Taste Cambodia's Richest Soils")
                        .param("heroSubtitle", "Directly sourced from trusted local farmers.")
                        .param("heroPrimaryBtnText", "Shop Vegetables")
                        .param("heroSecondaryBtnText", "Register as Farmer")
                        .param("exchangeRateUsdKhr", "4120.00")
                        .param("shippingFlatRate", "1.75")
                        .param("shippingFreeThreshold", "30.00")
                        .param("supportEmail", "contact@battambangfresh.kh")
                        .param("supportPhone", "+855 53 999 111")
                        .param("operatingHours", "Mon-Sun 6am-8pm")
                        .param("officeAddress", "Battambang Central Market, Cambodia"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/store-customize"))
                .andExpect(flash().attributeExists("successMessage"));

        StoreConfig updated = storeConfigService.getConfig();
        assertEquals("Battambang Fresh Direct", updated.getStoreName());
        assertEquals("Finest Rice & Organic Vegetables", updated.getTagline());
        assertEquals("#1e40af", updated.getBrandColor());
        assertTrue(updated.isAnnouncementEnabled());
        assertEquals("Free Delivery on orders above $20 across Battambang!", updated.getAnnouncementText());
        assertEquals(0, new BigDecimal("4120.00").compareTo(updated.getExchangeRateUsdKhr()));
        assertEquals(0, new BigDecimal("1.75").compareTo(updated.getShippingFlatRate()));
        assertEquals(0, new BigDecimal("30.00").compareTo(updated.getShippingFreeThreshold()));
        assertEquals("contact@battambangfresh.kh", updated.getSupportEmail());
    }

    @Test
    @DisplayName("Admin can reset store customization back to defaults")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testResetStoreCustomizeDefaults() throws Exception {
        mockMvc.perform(post("/admin/store-customize/reset")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/store-customize"))
                .andExpect(flash().attributeExists("successMessage"));

        StoreConfig config = storeConfigService.getConfig();
        assertEquals("Khmer Marketplace", config.getStoreName());
        assertEquals(0, new BigDecimal("4100").compareTo(config.getExchangeRateUsdKhr()));
        assertEquals(0, new BigDecimal("1.50").compareTo(config.getShippingFlatRate()));
        assertEquals(0, new BigDecimal("25.00").compareTo(config.getShippingFreeThreshold()));
    }

    @Test
    @DisplayName("Non-admin users cannot access store customize console")
    @WithMockUser(username = "buyer1", roles = {"BUYER"})
    void testNonAdminCannotAccessStoreCustomize() throws Exception {
        mockMvc.perform(get("/admin/store-customize"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/store-customize")
                        .with(csrf())
                        .param("storeName", "Hacked Name")
                        .param("heroTitle", "Hacked")
                        .param("exchangeRateUsdKhr", "4000")
                        .param("shippingFlatRate", "1.00")
                        .param("shippingFreeThreshold", "20.00"))
                .andExpect(status().isForbidden());
    }
}
