package com.example.khmer_market.admin;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.*;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AdminConsoleSecurityAndOperationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmerProfileRepository farmerProfileRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User adminUser;
    private User farmerUser;
    private FarmerProfile farmerProfile;
    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Setup Admin User
        adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setEmail("admin@khmermarket.gov.kh");
            u.setPassword("{bcrypt}$2a$12$e8YkY/mN0H2G998Z5E1R4eB.B7K0K5H9x5.mN.K.a4R6t7u8v9w0x");
            u.setFullName("Platform Administrator");
            u.setRole(UserRole.ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        // Setup Pending Farmer
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        farmerUser = new User();
        farmerUser.setUsername("pending_farmer_" + uniqueSuffix);
        farmerUser.setEmail("farmer_" + uniqueSuffix + "@mail.com");
        farmerUser.setPassword("{bcrypt}$2a$12$e8YkY/mN0H2G998Z5E1R4eB.B7K0K5H9x5.mN.K.a4R6t7u8v9w0x");
        farmerUser.setFullName("Pending Farmer");
        farmerUser.setPhone("+85512999" + uniqueSuffix.substring(0, 3));
        farmerUser.setRole(UserRole.FARMER);
        farmerUser.setStatus(UserStatus.PENDING);
        farmerUser = userRepository.save(farmerUser);

        farmerProfile = new FarmerProfile();
        farmerProfile.setUser(farmerUser);
        farmerProfile.setFarmName("Organic Eco Farm " + uniqueSuffix);
        farmerProfile.setFarmerName("Pending Farmer");
        farmerProfile.setProvince("Battambang");
        farmerProfile.setPhone(farmerUser.getPhone());
        farmerProfile = farmerProfileRepository.save(farmerProfile);

        // Setup Category
        testCategory = new Category();
        testCategory.setName("Admin Test Cat " + uniqueSuffix);
        testCategory.setDescription("Test category for admin operations");
        testCategory = categoryRepository.save(testCategory);

        // Setup Product
        testProduct = new Product();
        testProduct.setName("Test Admin Melon " + uniqueSuffix);
        testProduct.setCategory(testCategory);
        testProduct.setSeller(farmerUser);
        testProduct.setStatus(ProductStatus.ACTIVE);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("Unauthenticated user accessing /admin/dashboard must be redirected to /admin/login")
    void testUnauthenticatedRedirectToAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    @Test
    @WithMockUser(username = "buyer1", roles = {"BUYER"})
    @DisplayName("ROLE_BUYER user accessing /admin/dashboard must be rejected with 403 Forbidden")
    void testBuyerCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "farmer1", roles = {"FARMER"})
    @DisplayName("ROLE_FARMER user accessing /admin/dashboard must be rejected with 403 Forbidden")
    void testFarmerCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ROLE_ADMIN accessing /admin/dashboard succeeds with 200 OK")
    void testAdminAccessDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("stats", "recentAudits"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin JSON analytics API returns chart dataset")
    void testAdminAnalyticsApi() throws Exception {
        mockMvc.perform(get("/admin/api/analytics/charts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenueLabels").isArray())
                .andExpect(jsonPath("$.orderStatusLabels").isArray())
                .andExpect(jsonPath("$.roleLabels").isArray())
                .andExpect(jsonPath("$.categoryLabels").isArray())
                .andExpect(jsonPath("$.topFarmerLabels").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin farmer approval queue renders farmer info, username, and approval action buttons")
    void testListFarmersRendersApproveAndRejectActions() throws Exception {
        mockMvc.perform(get("/admin/farmers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/farmers"))
                .andExpect(model().attributeExists("farmers"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(farmerProfile.getFarmName())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(farmerUser.getUsername())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/farmers/" + farmerProfile.getId() + "/approve")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Approve")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reject")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin farmer detail page renders farm enterprise details, user record, and approve button")
    void testViewFarmerDetailRendersVerificationActions() throws Exception {
        mockMvc.perform(get("/admin/farmers/" + farmerProfile.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/farmer-detail"))
                .andExpect(model().attributeExists("farmer"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(farmerProfile.getFarmName())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(farmerUser.getUsername())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/farmers/" + farmerProfile.getId() + "/approve")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Approve & Activate Farmer")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin approving farmer updates user status to ACTIVE and records audit entry")
    void testApproveFarmerWorkflow() throws Exception {
        assertEquals(UserStatus.PENDING, farmerUser.getStatus());

        mockMvc.perform(post("/admin/farmers/" + farmerProfile.getId() + "/approve")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/farmers"));

        entityManager.flush();
        entityManager.clear();

        User refreshed = userRepository.findById(farmerUser.getId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, refreshed.getStatus());

        boolean auditFound = auditService.getRecentEvents().stream()
                .anyMatch(a -> a.action().equals("APPROVE_FARMER") && a.targetEntityId().equals(String.valueOf(farmerProfile.getId())));
        assertTrue(auditFound, "Structured audit log must be recorded for APPROVE_FARMER");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin rejecting farmer updates user status to SUSPENDED and records audit entry")
    void testRejectFarmerWorkflow() throws Exception {
        mockMvc.perform(post("/admin/farmers/" + farmerProfile.getId() + "/reject")
                        .param("reason", "Incomplete land title proof")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/farmers"));

        entityManager.flush();
        entityManager.clear();

        User refreshed = userRepository.findById(farmerUser.getId()).orElseThrow();
        assertEquals(UserStatus.SUSPENDED, refreshed.getStatus());

        boolean auditFound = auditService.getRecentEvents().stream()
                .anyMatch(a -> a.action().equals("REJECT_FARMER") && a.targetEntityId().equals(String.valueOf(farmerProfile.getId())));
        assertTrue(auditFound, "Structured audit log must be recorded for REJECT_FARMER");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin unlocking locked account resets failed attempts")
    void testUnlockUserAccount() throws Exception {
        UserSecurity sec = userSecurityRepository.findByUserId(farmerUser.getId())
                .orElseGet(() -> new UserSecurity(farmerUser));
        sec.setFailedAttempts(5);
        userSecurityRepository.save(sec);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/admin/users/" + farmerUser.getId() + "/unlock")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + farmerUser.getId()));

        entityManager.flush();
        entityManager.clear();

        UserSecurity refreshedSec = userSecurityRepository.findByUserId(farmerUser.getId()).orElseThrow();
        assertEquals(0, refreshedSec.getFailedAttempts());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin toggling product status to INACTIVE moderates catalog")
    void testProductModerationToggle() throws Exception {
        mockMvc.perform(post("/admin/products/" + testProduct.getId() + "/status")
                        .param("status", "INACTIVE")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products/" + testProduct.getId()));

        entityManager.flush();
        entityManager.clear();

        Product refreshed = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(ProductStatus.INACTIVE, refreshed.getStatus());
    }
}
