package com.frandm.notificationcenter.auth.controller;

import com.frandm.notificationcenter.auth.dto.AuthUserResponse;
import com.frandm.notificationcenter.auth.dto.LoginRequest;
import com.frandm.notificationcenter.security.service.AdminAuthService;
import com.frandm.notificationcenter.security.service.JwtService;
import com.frandm.notificationcenter.security.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AdminAuthService adminAuthService;
    private final JwtService jwtService;
    private final RateLimiterService rateLimiterService;
    private final String cookieName;
    private final boolean cookieSecure;
    private final long cookieMaxAge;

    public AuthController(
            AdminAuthService adminAuthService,
            JwtService jwtService,
            RateLimiterService rateLimiterService,
            @Value("${app.security.cookie-name:nc_session}") String cookieName,
            @Value("${app.security.cookie-secure:false}") boolean cookieSecure,
            @Value("${app.security.cookie-max-age:604800}") long cookieMaxAge
    ) {
        this.adminAuthService = adminAuthService;
        this.jwtService = jwtService;
        this.rateLimiterService = rateLimiterService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieMaxAge = cookieMaxAge;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        String clientIp = extractClientIp(servletRequest);
        String rateLimitKey = clientIp + ":" + request.username().trim();

        if (rateLimiterService.isBlocked(rateLimitKey)) {
            long remaining = rateLimiterService.getRemainingLockoutSeconds(rateLimitKey);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Too many failed attempts. Please try again in " + remaining + " seconds.",
                    "retryAfterSeconds", remaining
            ));
        }

        boolean authenticated = adminAuthService.authenticate(request.username(), request.password(), clientIp);
        if (!authenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Invalid username or password"
            ));
        }

        String jwtToken = jwtService.generateToken(request.username().trim());

        ResponseCookie cookie = ResponseCookie.from(cookieName, jwtToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(cookieMaxAge)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(new AuthUserResponse(true, request.username().trim()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return ResponseEntity.ok(new AuthUserResponse(true, auth.getName()));
        }
        return ResponseEntity.ok(new AuthUserResponse(false, null));
    }

    private String extractClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
