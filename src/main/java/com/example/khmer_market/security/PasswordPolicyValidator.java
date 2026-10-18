package com.example.khmer_market.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Component
public class PasswordPolicyValidator {

    private static final Logger log = LoggerFactory.getLogger(PasswordPolicyValidator.class);

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private final Set<String> commonPasswords = new HashSet<>();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("security/top-passwords.txt");
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                            commonPasswords.add(trimmed.toLowerCase());
                        }
                    }
                }
            }
            log.info("Loaded {} breached/common passwords for offline policy validation", commonPasswords.size());
        } catch (Exception e) {
            log.error("Failed to load top-passwords.txt", e);
        }
    }

    public void validatePassword(String password) throws IllegalArgumentException {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Password must not exceed " + MAX_LENGTH + " characters");
        }
        if (commonPasswords.contains(password.toLowerCase())) {
            throw new IllegalArgumentException("This password is too common or appears in known data breaches. Please choose a more secure password.");
        }
    }

    public boolean isValid(String password) {
        try {
            validatePassword(password);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
