
package com.fooddelivery.auth_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.secret}")
    private String secret;

    /**
     * This method determines whether this JWT filter should be applied to the current request.
     * We explicitly tell it NOT to filter (skip) requests to public authentication endpoints
     * and Swagger UI/API documentation paths. This ensures these paths are accessible without JWT.
     *
     * @param request The current HttpServletRequest.
     * @return true if the filter should NOT be applied (i.e., skip this filter), false otherwise.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        logger.debug("Evaluating path for JWT filter: {}", path);

        // Skip filter for registration, login, Swagger UI, and OpenAPI documentation paths
        // This must match the public paths configured in SecurityConfig's webSecurityCustomizer
        return path.equals("/auth/register") ||
               path.equals("/auth/login") ||
               path.startsWith("/swagger-ui.html") || // Exact HTML path for swagger-ui.html
               path.startsWith("/swagger-ui/") ||    // Swagger UI resources (CSS, JS, etc.)
               path.equals("/v3/api-docs") ||       // Exact path for the main OpenAPI JSON definition
               path.startsWith("/v3/api-docs/") ||   // OpenAPI JSON sub-paths (like /v3/api-docs/swagger-config)
               path.startsWith("/actuator/");         // Actuator endpoints (if you have them)
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // This block will now only be executed for paths that shouldNotFilter returns 'false' for.
        // For public paths (register, login, Swagger, Actuator), this method will be skipped entirely.

        String authHeader = request.getHeader("Authorization");
        logger.debug("Authorization header: {}", authHeader != null ? "Present" : "Missing");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for a secured endpoint. Returning 401 Unauthorized.");
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

            // Extract roles claim. Assuming roles are stored as a List of strings in the 'roles' claim.
            // Default to an empty list if no roles claim is present.
            List<?> rolesRaw = claims.get("roles", List.class);
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();
            if (rolesRaw != null) {
                authorities = rolesRaw.stream()
                        // Ensure "ROLE_" prefix for Spring Security's role checks
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + ((String) role).toUpperCase()))
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
            logger.error("Invalid JWT signature: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.ExpiredJwtException | io.jsonwebtoken.UnsupportedJwtException | IllegalArgumentException e) {
            // Catch more specific JWT exceptions
            logger.error("Invalid JWT token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception e) {
            logger.error("Error processing JWT: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // Use 500 for unexpected errors during processing
            return;
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}