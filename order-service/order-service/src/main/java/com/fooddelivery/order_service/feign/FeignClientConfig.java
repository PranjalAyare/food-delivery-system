package com.fooddelivery.order_service.feign;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt; // For Spring Security 5/6+ with JWT

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Get the current Authentication object from Spring Security Context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                // Check if the principal is an instance of Jwt (common with Spring Security's OAuth2 resource server)
                if (authentication.getPrincipal() instanceof Jwt) {
                    Jwt jwt = (Jwt) authentication.getPrincipal();
                    // Add the Authorization header with the Bearer token
                    requestTemplate.header("Authorization", "Bearer " + jwt.getTokenValue());
                    System.out.println("✅ Feign Request Interceptor: Forwarding JWT token.");
                }
                // Optional: If you're using a custom UserDetails object that stores the raw token,
                // you might need to adapt this logic to extract the token string.
                // For example, if token is stored in credentials:
                // else if (authentication.getCredentials() instanceof String) {
                //    requestTemplate.header("Authorization", "Bearer " + authentication.getCredentials());
                // }
                // Another common pattern is to store the token in a custom UserDetails object or a ThreadLocal.
            } else {
                System.out.println("⚠️ Feign Request Interceptor: No JWT token found in SecurityContext for current request.");
                // This might happen for internal service-to-service calls that aren't
                // triggered by an authenticated user, or if the token is handled differently.
                // Depending on your security requirements, you might
                // - allow the call to proceed without a token (if the endpoint is unsecured)
                // - throw an exception (if the call should always be authenticated)
                // - provide a system-level token (for internal service communication)
            }
        };
    }
}