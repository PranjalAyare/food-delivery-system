package com.fooddelivery.restaurant_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint; // Import HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Test-specific Spring Security configuration for RestaurantControllerTest.
 * This configuration is designed to work within the @WebMvcTest slice.
 * It enables method security (@PreAuthorize) and defines basic authorization rules
 * suitable for mocking authentication via Spring Security Test utilities.
 * It explicitly does NOT include the JwtFilter, as authentication is handled by
 * MockMvc's .with(user(...)) in tests.
 */
@TestConfiguration // Marks this class as a source of bean definitions for tests
@EnableMethodSecurity(prePostEnabled = true) // Crucial for enabling @PreAuthorize in tests
public class TestSecurityConfig {

    /**
     * Configures the security filter chain for test contexts.
     * - Disables CSRF protection (common for REST API testing convenience).
     * - Configures authorization rules similar to the main SecurityConfig.
     * - Permits Swagger/API docs for testing convenience.
     * - Requires ADMIN role for POST/DELETE operations on /restaurants.
     * - Authenticates all other requests.
     * - Sets session management to stateless, reflecting the production JWT setup.
     * - Does NOT add the JwtFilter, as authentication is mocked.
     * - Explicitly disables anonymous authentication to ensure 401 Unauthorized for unauthenticated requests.
     * - Adds an authentication entry point to return 401 for unauthenticated access.
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for easier testing
                .authorizeHttpRequests(auth -> auth
                        // Permit Swagger/API docs for testing convenience
                        .requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/actuator/**"
                        ).permitAll()
                        // Only ADMIN can create restaurants (POST /restaurants)
                        .requestMatchers(HttpMethod.POST, "/restaurants").hasRole("ADMIN")
                        // Only ADMIN can delete restaurants (DELETE /restaurants/{id})
                        .requestMatchers(HttpMethod.DELETE, "/restaurants/{id}").hasRole("ADMIN")
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Stateless sessions for JWT
                )
                // Explicitly disable anonymous authentication to ensure 401 for unauthenticated access
                .anonymous(AbstractHttpConfigurer::disable)
                // Add authentication entry point to return 401 Unauthorized for unauthenticated requests
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // IMPORTANT: Do NOT add jwtFilter here, as authentication is handled by MockMvc.with(user())
                // .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // REMOVED
                .build();
    }
}
