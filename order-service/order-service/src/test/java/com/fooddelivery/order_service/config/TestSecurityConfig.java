package com.fooddelivery.order_service.config;

import org.springframework.boot.test.context.TestConfiguration; // Use TestConfiguration for test-specific beans
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Test-specific Spring Security configuration for OrderControllerTest.
 * This configuration is designed to work within the @WebMvcTest slice.
 * It ignores Swagger paths and explicitly disables anonymous authentication
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
            "/swagger-ui/**"
        );
    }

    /**
     * Defines the security filter chain for test contexts.
     * - Disables CSRF.
     * - Requires authentication for /orders/** and any other request not ignored.
     * - Sets 401 Unauthorized for unauthenticated access.
     * - Explicitly disables anonymous authentication to ensure 401 instead of 403.
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for API testing convenience
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/orders/**").authenticated() // /orders endpoints require authentication
                        .anyRequest().authenticated() // All other requests not explicitly ignored also require authentication
                )
                .exceptionHandling(eh -> eh
                        // For unauthenticated requests, return a 401 Unauthorized status
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // Explicitly disable anonymous authentication.
                // This is crucial for distinguishing between 401 (unauthenticated)
                // and 403 (authenticated but unauthorized/forbidden).
                .anonymous(AbstractHttpConfigurer::disable)
                .build();
    }
}