package com.example.khmer_market.security;

import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.entity.UserSecurity;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.repository.UserSecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PasswordPolicyAndLockoutTest {

    @Autowired
    private PasswordPolicyValidator validator;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testlockoutuser");
        testUser.setEmail("lockout@example.com");
        testUser.setPassword("ValidSecurePassword2026!");
        testUser.setRole(UserRole.BUYER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser = userRepository.saveAndFlush(testUser);

        UserSecurity sec = new UserSecurity(testUser);
        userSecurityRepository.saveAndFlush(sec);
    }

    @Test
    @DisplayName("Password policy should reject passwords shorter than 12 characters")
    void testPasswordTooShort() {
        assertFalse(validator.isValid("Short1!"));
        assertThrows(IllegalArgumentException.class, () -> validator.validatePassword("Short1!"));
    }

    @Test
    @DisplayName("Password policy should accept valid 12+ character Unicode and Khmer passwords")
    void testPasswordValidUnicodeAndKhmer() {
        assertTrue(validator.isValid("ពាក្យសម្ងាត់សុវត្ថិភាពខ្ពស់២០២៦"));
        assertTrue(validator.isValid("Correct Horse Battery Staple 2026!"));
    }

    @Test
    @DisplayName("Password policy should reject common/breached passwords from offline dictionary")
    void testPasswordCommonRejected() {
        assertFalse(validator.isValid("123456789012"));
        assertFalse(validator.isValid("password12345"));
        assertFalse(validator.isValid("administrator"));
    }

    @Test
    @DisplayName("Account lockout after 5 consecutive failed logins")
    void testAccountLockoutAfterFiveFailures() {
        String username = testUser.getUsername();

        // 4 failed attempts should not lock yet
        for (int i = 1; i <= 4; i++) {
            Long lockTime = loginAttemptService.loginFailed(username);
            assertNull(lockTime, "Attempt " + i + " should not lock the account");
            assertNull(loginAttemptService.getRemainingLockSeconds(username));
        }

        // 5th failed attempt must trigger lockout
        Long lockTime = loginAttemptService.loginFailed(username);
        assertNotNull(lockTime, "5th failed attempt must lock account");
        assertTrue(lockTime > 0);

        Long remainingSeconds = loginAttemptService.getRemainingLockSeconds(username);
        assertNotNull(remainingSeconds);
        assertTrue(remainingSeconds > 0 && remainingSeconds <= 900);

        // Success resets the counter once lock is cleared or on success
        loginAttemptService.loginSucceeded(username);
        assertNull(loginAttemptService.getRemainingLockSeconds(username));
    }
}
