package com.frandm.notificationcenter.security;

import com.frandm.notificationcenter.security.service.AdminAuthService;
import com.frandm.notificationcenter.security.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminAuthServiceTest {

    private AdminAuthService adminAuthService;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        adminAuthService = new AdminAuthService("admin", null, "testpassword123", encoder, rateLimiterService);
    }

    @Test
    void shouldAuthenticateValidCredentials() {
        boolean valid = adminAuthService.authenticate("admin", "testpassword123", "127.0.0.1");
        assertThat(valid).isTrue();
    }

    @Test
    void shouldRejectInvalidCredentials() {
        boolean invalid = adminAuthService.authenticate("admin", "wrong_password", "127.0.0.1");
        assertThat(invalid).isFalse();
    }

    @Test
    void shouldRateLimitAfterMultipleFailures() {
        String ip = "192.168.1.100";
        for (int i = 0; i < 5; i++) {
            adminAuthService.authenticate("admin", "wrong", ip);
        }

        assertThat(rateLimiterService.isBlocked(ip + ":admin")).isTrue();
        boolean blockedResult = adminAuthService.authenticate("admin", "testpassword123", ip);
        assertThat(blockedResult).isFalse();
    }
}
