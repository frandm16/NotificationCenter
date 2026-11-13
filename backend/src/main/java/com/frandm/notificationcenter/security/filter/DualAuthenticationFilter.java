package com.frandm.notificationcenter.security.filter;

import com.frandm.notificationcenter.security.model.AdminAuthenticationToken;
import com.frandm.notificationcenter.security.model.ApiTokenAuthenticationToken;
import com.frandm.notificationcenter.security.service.AdminAuthService;
import com.frandm.notificationcenter.security.service.JwtService;
import com.frandm.notificationcenter.token.model.ApiToken;
import com.frandm.notificationcenter.token.service.ApiTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DualAuthenticationFilter extends OncePerRequestFilter {

    private final ApiTokenService apiTokenService;
    private final JwtService jwtService;
    private final AdminAuthService adminAuthService;
    private final String cookieName;

    public DualAuthenticationFilter(
            ApiTokenService apiTokenService,
            JwtService jwtService,
            AdminAuthService adminAuthService,
            @Value("${app.security.cookie-name:nc_session}") String cookieName
    ) {
        this.apiTokenService = apiTokenService;
        this.jwtService = jwtService;
        this.adminAuthService = adminAuthService;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (processToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    String jwtToken = cookie.getValue();
                    if (processJwtToken(jwtToken)) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                }
            }
        }

        String requestUri = request.getRequestURI();
        if (requestUri != null && requestUri.contains("/stream")) {
            String queryToken = request.getParameter("token");
            if (queryToken != null && !queryToken.isBlank()) {
                if (processToken(queryToken.trim())) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean processToken(String token) {
        if (token.startsWith("nc_live_")) {
            Optional<ApiToken> apiTokenOpt = apiTokenService.validateAndTouch(token);
            if (apiTokenOpt.isPresent()) {
                ApiToken apiToken = apiTokenOpt.get();
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_SERVICE"));
                for (String scope : apiToken.getScopeSet()) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                }
                ApiTokenAuthenticationToken authentication = new ApiTokenAuthenticationToken(apiToken, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return true;
            }
            return false;
        }

        return processJwtToken(token);
    }

    private boolean processJwtToken(String jwtToken) {
        Optional<String> usernameOpt = jwtService.validateAndExtractUsername(jwtToken);
        if (usernameOpt.isPresent()) {
            String username = usernameOpt.get();
            if (adminAuthService.getExpectedUsername().equals(username)) {
                AdminAuthenticationToken authentication = new AdminAuthenticationToken(
                        username,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return true;
            }
        }
        return false;
    }
}
