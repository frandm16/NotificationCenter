package com.frandm.notificationcenter.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final String expectedUsername;
    private final String passwordHash;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;

    public AdminAuthService(
            @Value("${app.security.admin-username:admin}") String expectedUsername,
            @Value("${app.security.admin-password-hash:}") String configuredHash,
            @Value("${app.security.admin-password:change_me_immediately}") String plainPassword,
            PasswordEncoder passwordEncoder,
            RateLimiterService rateLimiterService
    ) {
        this.expectedUsername = expectedUsername != null ? expectedUsername.trim() : "admin";
        this.passwordEncoder = passwordEncoder;
        this.rateLimiterService = rateLimiterService;

        if (configuredHash != null && !configuredHash.isBlank()) {
            this.passwordHash = configuredHash.trim();
        } else {
            this.passwordHash = passwordEncoder.encode(plainPassword);
        }
    }

    public boolean authenticate(String username, String password, String clientIp) {
        String rateLimitKey = clientIp + ":" + (username != null ? username.trim() : "");

        if (rateLimiterService.isBlocked(rateLimitKey)) {
            return false;
        }

        if (username == null || password == null) {
            rateLimiterService.recordFailure(rateLimitKey);
            return false;
        }

        boolean usernameMatches = expectedUsername.equals(username.trim());
        boolean passwordMatches = passwordEncoder.matches(password, passwordHash);

        if (usernameMatches && passwordMatches) {
            rateLimiterService.recordSuccess(rateLimitKey);
            return true;
        }

        rateLimiterService.recordFailure(rateLimitKey);
        return false;
    }

    public String getExpectedUsername() {
        return expectedUsername;
    }
}
