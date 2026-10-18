package com.example.khmer_market.security;

import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.entity.UserSecurity;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.repository.UserSecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    @Value("${market.security.lockout.max-failed-attempts:5}")
    private int maxFailedAttempts = 5;

    @Value("${market.security.lockout.lock-duration-minutes:15}")
    private int lockDurationMinutes = 15;

    private final UserRepository userRepository;
    private final UserSecurityRepository userSecurityRepository;

    public LoginAttemptService(UserRepository userRepository,
                               UserSecurityRepository userSecurityRepository) {
        this.userRepository = userRepository;
        this.userSecurityRepository = userSecurityRepository;
    }

    @Transactional
    public void loginSucceeded(String usernameOrEmail) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Optional<UserSecurity> secOpt = userSecurityRepository.findByUserId(user.getId());
            if (secOpt.isPresent()) {
                UserSecurity sec = secOpt.get();
                sec.setFailedAttempts(0);
                sec.setLockedUntil(null);
                userSecurityRepository.save(sec);
            }
        }
    }

    @Transactional
    public Long loginFailed(String usernameOrEmail) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail);
        if (userOpt.isEmpty()) {
            log.warn("Login failed for non-existent account: {}", mask(usernameOrEmail));
            return null;
        }

        User user = userOpt.get();
        UserSecurity sec = userSecurityRepository.findByUserId(user.getId())
                .orElseGet(() -> new UserSecurity(user));

        // If lock period has expired, reset counter first
        if (sec.getLockedUntil() != null && sec.getLockedUntil().isBefore(LocalDateTime.now())) {
            sec.setFailedAttempts(0);
            sec.setLockedUntil(null);
        }

        sec.setFailedAttempts(sec.getFailedAttempts() + 1);

        if (sec.getFailedAttempts() >= maxFailedAttempts) {
            sec.setLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            userSecurityRepository.save(sec);
            log.warn("Security Event: Account locked due to {} failed login attempts for user ID: {}",
                    sec.getFailedAttempts(), user.getId());
            return (long) lockDurationMinutes * 60;
        }

        userSecurityRepository.save(sec);
        log.warn("Security Event: Failed login attempt {} of {} for user ID: {}",
                sec.getFailedAttempts(), maxFailedAttempts, user.getId());
        return null;
    }

    @Transactional(readOnly = true)
    public Long getRemainingLockSeconds(String usernameOrEmail) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail);
        if (userOpt.isPresent()) {
            Optional<UserSecurity> secOpt = userSecurityRepository.findByUserId(userOpt.get().getId());
            if (secOpt.isPresent()) {
                UserSecurity sec = secOpt.get();
                if (sec.isAccountLocked()) {
                    long seconds = Duration.between(LocalDateTime.now(), sec.getLockedUntil()).getSeconds();
                    return Math.max(0, seconds);
                }
            }
        }
        return null;
    }

    private String mask(String input) {
        if (input == null || input.length() < 3) return "***";
        return input.substring(0, 2) + "***";
    }
}
