// In order-service/src/main/java/com/fooddelivery/order_service/security/JwtFilter.java

package com.fooddelivery.order_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // Import for @Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    // Inject the secret key from application.properties
    @Value("${jwt.secret}")
    private String secret;

    /**
     * This method determines whether this JWT filter should be applied to the current request.
     * We explicitly tell it NOT to filter (skip) requests to Swagger UI and API documentation paths.
     * This provides an extra layer of robustness even though WebSecurityCustomizer handles it.
     *
     * @param request The current HttpServletRequest.
     * @return true if the filter should NOT be applied (i.e., skip this filter), false otherwise.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        logger.debug("Evaluating path for JWT filter: {}", path);

        // Skip filter for Swagger UI and OpenAPI documentation paths
        // These paths should match what's ignored in SecurityConfig's WebSecurityCustomizer
        return path.startsWith("/swagger-ui.html") || // Exact HTML path
               path.startsWith("/swagger-ui/") ||    // Swagger UI resources (CSS, JS)
               path.equals("/v3/api-docs") ||       // Exact path for the main OpenAPI JSON definition
               path.startsWith("/v3/api-docs/") ||   // OpenAPI JSON sub-paths (e.g., /v3/api-docs/swagger-config)
               path.startsWith("/actuator/");         // Actuator endpoints (if enabled and public)
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // This block will only be executed for paths that shouldNotFilter returns 'false' for.
        // Public paths (Swagger, Actuator) will bypass this method entirely.

        String authHeader = request.getHeader("Authorization");
        logger.debug("Authorization header: {}", authHeader != null ? "Present" : "Missing");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for a secured endpoint: {}. Returning 401 Unauthorized.", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return; // Stop the filter chain
        }

        try {
            String token = authHeader.substring(7); // Extract the token after "Bearer "

            // Parse the JWT token to get claims
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secret.getBytes()) // Use the injected secret key
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            logger.debug("JWT parsed successfully for subject: {}", claims.getSubject());

            String username = claims.getSubject(); // Subject of the JWT is typically the username/email

            // Extract roles claim - expects a list of roles as strings, e.g., ["ROLE_USER", "ROLE_ADMIN"]
            // The claims.get("roles", List.class) correctly retrieves the list of strings.
            List<?> rolesRaw = claims.get("roles", List.class);
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();

            if (rolesRaw != null) {
                authorities = rolesRaw.stream()
                        // Ensure each role string is properly cast and then converted to SimpleGrantedAuthority
                        .map(role -> new SimpleGrantedAuthority((String) role))
                        .collect(Collectors.toList());
            }

            // If username is present and no authentication is currently set in the context
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Create an Authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                // Set the authentication in the SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authentication set for user: {} with authorities: {}", username, authorities);
            }

        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.error("Invalid JWT signature for request to {}: {}", request.getRequestURI(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.ExpiredJwtException | io.jsonwebtoken.UnsupportedJwtException | IllegalArgumentException e) {
            // Catch more specific JWT exceptions for better logging
            logger.error("Invalid or malformed JWT token for request to {}: {}", request.getRequestURI(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception e) {
            logger.error("Unexpected error processing JWT for request to {}: {}", request.getRequestURI(), e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // Use 500 for unexpected errors during processing
            return;
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}