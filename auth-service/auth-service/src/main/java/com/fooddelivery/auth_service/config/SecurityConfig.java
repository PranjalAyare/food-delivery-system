
package com.fooddelivery.auth_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Import HttpMethod
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    /**
     * This bean configures paths to be COMPLETELY ignored by the Spring Security filter chain.
     * Requests to these paths will not go through any security filters, including JwtAuthFilter.
     * This is the most reliable way to ensure /auth/register and /auth/login are truly public,
     * and also to ensure the /error path does not get secured if internal forwards happen.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        log.info("Configuring WebSecurityCustomizer to ignore /auth/**, Swagger, Actuator, and /error paths.");
        return (web) -> web.ignoring()
                           .requestMatchers(HttpMethod.POST, "/auth/register") // Explicitly ignore POST for registration
                           .requestMatchers(HttpMethod.POST, "/auth/login")    // Explicitly ignore POST for login
                           .requestMatchers(
                               "/swagger-ui.html",         // Ignore Swagger UI
                               "/swagger-ui/**",           // Ignore Swagger UI resources
                               "/v3/api-docs/**",          // Ignore OpenAPI documentation
                               "/actuator/**",             // Ignore Actuator endpoints
                               "/error"                    // Explicitly ignore the /error path
                           );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring SecurityFilterChain for Auth Service.");
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API-based authentication
                .authorizeHttpRequests(auth -> auth
                        // All requests NOT ignored by webSecurityCustomizer will require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Use stateless sessions for JWT
                )
                // Configure exception handling for unauthenticated requests to return 401
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // Add the custom JWT filter before Spring Security's default UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Use BCrypt for password hashing
    }
}
