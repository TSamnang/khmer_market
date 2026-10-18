package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.PasswordResetToken;
import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.entity.UserSecurity;
import com.example.khmer_market.repository.PasswordResetTokenRepository;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.repository.UserSecurityRepository;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.security.PasswordPolicyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final MailService mailService;
    private final SessionRegistry sessionRegistry;

    @Value("${market.security.password-reset.token-validity-minutes:30}")
    private int tokenValidityMinutes = 30;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                UserSecurityRepository userSecurityRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                PasswordPolicyValidator passwordPolicyValidator,
                                MailService mailService,
                                SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.mailService = mailService;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional
    public void createPasswordResetToken(String email, String appBaseUrl) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Anti-enumeration: Silent return
            log.info("Password reset requested for non-existent email");
            return;
        }

        User user = userOpt.get();

        // Invalidate prior unused tokens for this user
        tokenRepository.invalidateTokensForUser(user.getId(), LocalDateTime.now());

        // Generate 256-bit random token
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenValidityMinutes);

        PasswordResetToken resetToken = new PasswordResetToken(user, tokenHash, expiresAt);
        tokenRepository.save(resetToken);

        String resetLink = appBaseUrl + "/reset-password?token=" + rawToken;
        mailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        log.info("Security Event: Password reset token generated for user ID: {}", user.getId());
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        String tokenHash = hashToken(rawToken);
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenHash(tokenHash);
        if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
            return false;
        }

        passwordPolicyValidator.validatePassword(newPassword);

        PasswordResetToken token = tokenOpt.get();
        User user = token.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        UserSecurity sec = userSecurityRepository.findByUserId(user.getId())
                .orElseGet(() -> new UserSecurity(user));
        sec.setPasswordChangedAt(LocalDateTime.now());
        sec.setFailedAttempts(0);
        sec.setLockedUntil(null);
        userSecurityRepository.save(sec);

        // Invalidate all active sessions for this user
        invalidateUserSessions(user.getUsername());

        log.info("Security Event: Password successfully reset and sessions invalidated for user ID: {}", user.getId());
        return true;
    }

    public void invalidateUserSessions(String username) {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (principal instanceof CustomUserDetails userDetails) {
                if (userDetails.getUsername().equalsIgnoreCase(username)) {
                    List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                    for (SessionInformation session : sessions) {
                        session.expireNow();
                    }
                }
            }
        }
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
