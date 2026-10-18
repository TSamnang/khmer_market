package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.FarmerProfile;
import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.entity.UserSecurity;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.repository.FarmerProfileRepository;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.repository.UserSecurityRepository;
import com.example.khmer_market.security.PasswordPolicyValidator;
import com.example.khmer_market.web.dto.ChangePasswordDto;
import com.example.khmer_market.web.dto.RegisterDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final PasswordResetService passwordResetService;

    public UserService(UserRepository userRepository,
                       UserSecurityRepository userSecurityRepository,
                       FarmerProfileRepository farmerProfileRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicyValidator passwordPolicyValidator,
                       PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.farmerProfileRepository = farmerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.passwordResetService = passwordResetService;
    }

    @Transactional
    public User registerUser(RegisterDto dto) {
        // Validation of password policy
        passwordPolicyValidator.validatePassword(dto.getPassword());

        if (userRepository.existsByUsername(dto.getUsername().trim())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("Email address is already registered");
        }

        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName() != null ? dto.getFullName().trim() : dto.getUsername().trim());
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);

        // Security: Role enforcement (Admin cannot be registered through public registration UI)
        if ("farmer".equalsIgnoreCase(dto.getRole())) {
            user.setRole(UserRole.FARMER);
            // Farmers start in pending status until approved by admin
            user.setStatus(UserStatus.PENDING);
        } else {
            user.setRole(UserRole.BUYER);
            user.setStatus(UserStatus.ACTIVE);
        }

        User savedUser = userRepository.saveAndFlush(user);

        UserSecurity security = new UserSecurity(savedUser);
        userSecurityRepository.saveAndFlush(security);

        if (user.getRole() == UserRole.FARMER) {
            FarmerProfile profile = new FarmerProfile();
            profile.setUser(savedUser);
            profile.setFarmName(dto.getFarmName() != null && !dto.getFarmName().isBlank() ? dto.getFarmName().trim() : savedUser.getFullName() + "'s Farm");
            profile.setFarmerName(dto.getFarmerName() != null && !dto.getFarmerName().isBlank() ? dto.getFarmerName().trim() : savedUser.getFullName());
            profile.setPhone(savedUser.getPhone());
            profile.setProvince(dto.getProvince() != null ? dto.getProvince().trim() : "Phnom Penh");
            profile.setAddress(dto.getFarmAddress() != null ? dto.getFarmAddress().trim() : "");
            profile.setFarmDescription(dto.getFarmDescription());
            farmerProfileRepository.save(profile);
            log.info("Registered new farmer (pending approval): ID {}", savedUser.getId());
        } else {
            log.info("Registered new buyer: ID {}", savedUser.getId());
        }

        return savedUser;
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        passwordPolicyValidator.validatePassword(dto.getNewPassword());

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        UserSecurity sec = userSecurityRepository.findByUserId(userId)
                .orElseGet(() -> new UserSecurity(user));
        sec.setPasswordChangedAt(LocalDateTime.now());
        sec.setFailedAttempts(0);
        userSecurityRepository.save(sec);

        passwordResetService.invalidateUserSessions(user.getUsername());
        log.info("Security Event: User ID {} changed password successfully. Sessions invalidated.", userId);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
