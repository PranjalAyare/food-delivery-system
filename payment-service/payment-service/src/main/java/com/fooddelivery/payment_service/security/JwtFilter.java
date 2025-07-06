package com.fooddelivery.payment_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
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
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Value("${jwt.secret}")
    private String secret;

    /**
     * This method determines whether this JWT filter should be applied to the current request.
     * It explicitly tells the filter to skip requests to Swagger UI and API documentation paths.
     * This provides an extra layer of robustness even though WebSecurityCustomizer in SecurityConfig
     * also handles ignoring these paths.
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
            return;
        }

        try {
            String token = authHeader.substring(7);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secret.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            logger.debug("JWT parsed successfully for subject: {}", claims.getSubject());

            String username = claims.getSubject();

            List<?> rolesRaw = claims.get("roles", List.class);
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();
            if (rolesRaw != null) {
                authorities = rolesRaw.stream()
                        // Ensure each role string is properly cast and then converted to SimpleGrantedAuthority
                        // Assuming roles in JWT are simple strings like "ADMIN", "USER" (not "ROLE_ADMIN")
                        // If your Auth Service emits roles like "ADMIN", "USER", use "ROLE_" prefix here.
                        // If it emits "ROLE_ADMIN", "ROLE_USER", then just use `new SimpleGrantedAuthority((String) role)`
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + ((String) role).toUpperCase()))
                        .collect(Collectors.toList());
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authentication set for user: {} with authorities: {}", username, authorities);
            }

        } catch (SignatureException e) {
            logger.error("Invalid JWT signature for request to {}: {}", request.getRequestURI(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            logger.error("Invalid or malformed JWT token for request to {}: {}", request.getRequestURI(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception e) {
            logger.error("Unexpected error processing JWT for request to {}: {}", request.getRequestURI(), e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        filterChain.doFilter(request, response);
    }
}

