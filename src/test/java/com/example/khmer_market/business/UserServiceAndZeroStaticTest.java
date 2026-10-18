package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.FarmerProfile;
import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.repository.FarmerProfileRepository;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.service.UserService;
import com.example.khmer_market.web.dto.RegisterDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceAndZeroStaticTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmerProfileRepository farmerProfileRepository;

    @Test
    @DisplayName("Verify legacy static demo users are not seeded")
    void testNoLegacyStaticUsersSeeded() {
        assertFalse(userRepository.findByUsername("farmer_sokha").isPresent());
        assertFalse(userRepository.findByUsername("farmer_vanna").isPresent());
        assertFalse(userRepository.findByUsername("farmer_dara").isPresent());
        assertFalse(userRepository.findByUsername("buyer_bopha").isPresent());
        assertFalse(userRepository.findByUsername("buyer_rithy").isPresent());
        assertFalse(userRepository.findByUsername("buyer_chan").isPresent());
    }

    @Test
    @DisplayName("Dynamically register new Buyer user")
    void testDynamicBuyerRegistration() {
        RegisterDto dto = new RegisterDto();
        dto.setUsername("dynamic_buyer_1");
        dto.setEmail("dynamic_buyer_1@example.com");
        dto.setPassword("SecureBuyer2026!Password");
        dto.setConfirmPassword("SecureBuyer2026!Password");
        dto.setFullName("Sophea Chea");
        dto.setPhone("+85512111222");
        dto.setRole("buyer");

        User registered = userService.registerUser(dto);
        assertNotNull(registered.getId());
        assertEquals(UserRole.BUYER, registered.getRole());
        assertEquals(UserStatus.ACTIVE, registered.getStatus());
        assertEquals("dynamic_buyer_1", registered.getUsername());
    }

    @Test
    @DisplayName("Dynamically register new Farmer user")
    void testDynamicFarmerRegistration() {
        RegisterDto dto = new RegisterDto();
        dto.setUsername("dynamic_farmer_1");
        dto.setEmail("dynamic_farmer_1@example.com");
        dto.setPassword("SecureFarmer2026!Pass");
        dto.setConfirmPassword("SecureFarmer2026!Pass");
        dto.setFullName("Vireak Seng");
        dto.setPhone("+85512333444");
        dto.setRole("farmer");
        dto.setFarmName("Battambang Sunrise Organics");
        dto.setFarmerName("Vireak Seng");
        dto.setProvince("Battambang");
        dto.setFarmAddress("Sangkat Chamkar Samraong, Battambang");
        dto.setFarmDescription("Heritage rice and organic vegetables");

        User registered = userService.registerUser(dto);
        assertNotNull(registered.getId());
        assertEquals(UserRole.FARMER, registered.getRole());
        assertEquals(UserStatus.PENDING, registered.getStatus());

        Optional<FarmerProfile> profile = farmerProfileRepository.findByUserId(registered.getId());
        assertTrue(profile.isPresent());
        assertEquals("Battambang Sunrise Organics", profile.get().getFarmName());
        assertEquals("Battambang", profile.get().getProvince());
    }

    @Test
    @DisplayName("Public registration does not allow admin role")
    void testAdminRoleRejectedOnPublicRegistration() {
        RegisterDto dto = new RegisterDto();
        dto.setUsername("try_admin");
        dto.setEmail("try_admin@example.com");
        dto.setPassword("SuperAdmin2026!Secure");
        dto.setConfirmPassword("SuperAdmin2026!Secure");
        dto.setFullName("Unauthorized Admin Attempt");
        dto.setPhone("+85512555666");
        dto.setRole("admin");

        User registered = userService.registerUser(dto);
        assertNotNull(registered.getId());
        assertNotEquals(UserRole.ADMIN, registered.getRole());
        assertEquals(UserRole.BUYER, registered.getRole());
    }
}
