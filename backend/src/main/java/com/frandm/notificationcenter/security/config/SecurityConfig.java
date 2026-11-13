package com.frandm.notificationcenter.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frandm.notificationcenter.security.filter.DualAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final DualAuthenticationFilter dualAuthenticationFilter;
    private final String corsAllowedOrigin;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfig(
            DualAuthenticationFilter dualAuthenticationFilter,
            @Value("${app.security.cors-allowed-origin:http://localhost:7778}") String corsAllowedOrigin
    ) {
        this.dualAuthenticationFilter = dualAuthenticationFilter;
        this.corsAllowedOrigin = corsAllowedOrigin;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "status", 401,
                                    "error", "Unauthorized",
                                    "message", "Authentication required"
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "status", 403,
                                    "error", "Forbidden",
                                    "message", "Access denied"
                            ));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/logout", "/api/v1/auth/me").permitAll()
                        .requestMatchers("/api/v1/push/vapid-public-key").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications").hasAnyAuthority("SCOPE_notify", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/notifications/**").hasAnyAuthority("SCOPE_read", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/stream").hasAnyAuthority("SCOPE_read", "ROLE_ADMIN")
                        .requestMatchers("/api/v1/tokens/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/notifications/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications/read-all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/notifications/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/push/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(dualAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsAllowedOrigin.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Admin-Test-Source"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
