package com.fooddelivery.payment_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.config.http.SessionCreationPolicy; // For stateless sessions in tests

/**
 * Test-specific Spring Security configuration for PaymentControllerTest.
 * This configuration is designed to work within the @WebMvcTest slice.
 * It ignores Swagger and Actuator paths and explicitly disables anonymous authentication
 * to ensure unauthenticated requests correctly result in 401.
 * It does NOT include the JwtFilter, as @WebMvcTest uses @WithMockUser for authentication.
 */
@TestConfiguration // Indicates that this class provides configuration for tests
public class TestSecurityConfig {

/**
     * Configures paths to be completely ignored by the Spring Security filter chain.
     * Requests to these paths will bypass all security checks.
     */
@Bean
public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/actuator/**" // Also ignore actuator for tests
        );
}

/**
     * Defines the security filter chain for test contexts.
     * - Disables CSRF.
     * - Requires authentication for /payments/** and any other request not ignored.
     * - Sets 401 Unauthorized for unauthenticated access.
     * - Explicitly disables anonymous authentication to ensure 401 instead of 403.
     * - Sets session management to stateless for consistency with production setup.
     */
@Bean
public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for API testing convenience
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/payments/**").authenticated() // /payments endpoints require authentication
                        .anyRequest().authenticated() // All other requests not explicitly ignored also require authentication
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Use stateless sessions for JWT
                )
                .exceptionHandling(eh -> eh
                        // For unauthenticated requests, return a 401 Unauthorized status
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .anonymous(AbstractHttpConfigurer::disable) // Crucial for distinguishing 401 (unauthenticated) from 403 (forbidden)
                .build();
}
}
