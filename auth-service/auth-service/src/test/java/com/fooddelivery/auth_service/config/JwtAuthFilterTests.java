
package com.fooddelivery.auth_service.config;

// Removed import for com.fooddelivery.auth_service.model.User as it's not used by the filter's logic being tested
// Removed import for com.fooddelivery.auth_service.repository.UserRepository as it's not a dependency

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils; // To inject @Value secret
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm; // For generating dummy JWTs
import io.jsonwebtoken.security.Keys; // For generating strong keys
import java.security.Key; // For JWT signing key

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional; // Still needed for some general Java usage if any, but not for UserRepository now
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;


class JwtAuthFilterTest {

    // Removed @Mock private UserRepository userRepository; as it's not used by JwtAuthFilter

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks // Injects mocks into JwtAuthFilter
    private JwtAuthFilter jwtAuthFilter;

    // Hardcoded secret for testing - must be at least 32 bytes for HS256 to prevent SignatureException
    private final String TEST_SECRET = "thisismytestsecretkeyforjwtsigning1234567890abcdef";
    private Key signKey; // Key for signing JWTs, derived from TEST_SECRET

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext(); // Ensure SecurityContextHolder is clean before each test

        // Manually inject the secret into the JwtAuthFilter instance using ReflectionTestUtils.
        // This simulates Spring's @Value injection for unit tests where the full Spring context isn't loaded.
        ReflectionTestUtils.setField(jwtAuthFilter, "secret", TEST_SECRET);
        // Derive the signing key from the secret for generating test JWTs
        signKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
    }

    /**
     * Helper method to generate a valid JWT token for testing.
     * @param email The subject of the JWT.
     * @param roles A list of roles to include in the claims.
     * @param expirationMillis The duration in milliseconds for which the token is valid.
     * @return A signed JWT string.
     */
    private String generateToken(String email, List<String> roles, long expirationMillis) {
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", roles) // Add roles as a claim
                .setIssuedAt(new Date(System.currentTimeMillis())) // Set issued at current time
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis)) // Set expiration time
                .signWith(signKey, SignatureAlgorithm.HS256) // Sign with the test key
                .compact(); // Build and compact the JWT
    }

    /**
     * Helper method to generate an expired JWT token.
     * @param email The subject of the JWT.
     * @param roles A list of roles to include in the claims.
     * @return An expired JWT string.
     */
    private String generateExpiredToken(String email, List<String> roles) {
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", roles)
                .setIssuedAt(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1))) // Issued an hour ago
                .setExpiration(new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1))) // Expired a minute ago
                .signWith(signKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Helper method to generate a JWT token with a different (bad) signature.
     * This token will be rejected by the JwtAuthFilter due to signature mismatch.
     * @param email The subject of the JWT.
     * @param roles A list of roles to include in the claims.
     * @param expirationMillis The duration in milliseconds for which the token is valid.
     * @return A JWT string signed with a different key.
     */
    private String generateTokenWithBadSignature(String email, List<String> roles, long expirationMillis) {
        Key badKey = Keys.secretKeyFor(SignatureAlgorithm.HS256); // Generate a different key
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", roles)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(badKey, SignatureAlgorithm.HS256) // Sign with the bad key
                .compact();
    }

    // --- Tests for shouldNotFilter method ---
    /**
     * Verifies that the filter correctly identifies and skips filtering for the /auth/register path.
     */
    @Test
    void shouldNotFilter_ForAuthRegisterPath_ReturnsTrue() throws ServletException {
        when(request.getRequestURI()).thenReturn("/auth/register");
        assertTrue(jwtAuthFilter.shouldNotFilter(request));
    }

    /**
     * Verifies that the filter correctly identifies and skips filtering for the /auth/login path.
     */
    @Test
    void shouldNotFilter_ForAuthLoginPath_ReturnsTrue() throws ServletException {
        when(request.getRequestURI()).thenReturn("/auth/login");
        assertTrue(jwtAuthFilter.shouldNotFilter(request));
    }

    /**
     * Verifies that the filter does not skip filtering for other protected paths.
     */
    @Test
    void shouldNotFilter_ForOtherPaths_ReturnsFalse() throws ServletException {
        when(request.getRequestURI()).thenReturn("/restaurants");
        assertFalse(jwtAuthFilter.shouldNotFilter(request));
        when(request.getRequestURI()).thenReturn("/orders/123");
        assertFalse(jwtAuthFilter.shouldNotFilter(request));
    }


    // --- Tests for doFilterInternal method ---

    /**
     * Tests doFilterInternal when no Authorization header is present for a protected resource.
     * Expected: Returns 401 Unauthorized and stops the filter chain.
     */
    @Test
    void doFilterInternal_NoAuthHeader_ProtectedPath_ShouldReturnUnauthorized() throws Exception {
        // Mock a protected path where shouldNotFilter returns false
        when(request.getRequestURI()).thenReturn("/protected-resource");
        when(request.getHeader("Authorization")).thenReturn(null); // Simulate no Authorization header

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Verify that HttpServletResponse.SC_UNAUTHORIZED (401) is set as status
        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // Verify that the filter chain is NOT continued (because the filter returned early)
        verify(filterChain, never()).doFilter(request, response);
        // Verify no authentication is set in the SecurityContextHolder
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // No userRepository interaction to verify, as it's not a dependency
    }

    /**
     * Tests doFilterInternal when the Authorization header is present but does not start with "Bearer ".
     * Expected: Returns 401 Unauthorized and stops the filter chain.
     */
    @Test
    void doFilterInternal_InvalidBearerPrefix_ShouldReturnUnauthorized() throws Exception {
        when(request.getRequestURI()).thenReturn("/protected-resource");
        when(request.getHeader("Authorization")).thenReturn("Token some.invalid.token"); // Invalid prefix

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // No userRepository interaction to verify
    }

    /**
     * Tests doFilterInternal with a valid JWT token and a user found in the repository.
     * Expected: Authentication is set in SecurityContextHolder, and filter chain continues.
     */
    @Test
    void doFilterInternal_ValidToken_UserPresent_ShouldSetAuthentication() throws Exception {
        String email = "test@example.com";
        List<String> roles = Collections.singletonList("USER"); // Role(s) present in the JWT claim
        String validToken = generateToken(email, roles, TimeUnit.MINUTES.toMillis(10)); // Generate a valid token

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/protected-resource"); // Simulate a protected resource request
        // Removed: when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert that authentication is set correctly in SecurityContextHolder
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getName()); // Principal name is email (subject)
        // Verify authorities, ensuring "ROLE_" prefix is added by the filter
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));

        // Verify that the filter chain continues as expected
        verify(filterChain, times(1)).doFilter(request, response);
        // Verify that no unauthorized status was set by the filter
        verify(response, never()).setStatus(anyInt());
        // Removed: verify(userRepository, times(1)).findByEmail(email);
    }

    /**
     * Tests doFilterInternal with a valid JWT token for an ADMIN user.
     * Expected: Authentication set with ADMIN role, and filter chain continues.
     */
    @Test
    void doFilterInternal_ValidToken_AdminUserPresent_ShouldSetAuthentication() throws Exception {
        String email = "admin@example.com";
        List<String> roles = Collections.singletonList("ADMIN"); // Admin role in JWT
        String validToken = generateToken(email, roles, TimeUnit.MINUTES.toMillis(10));

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/admin-resource"); // Simulate an admin resource request
        // Removed: when(userRepository.findByEmail(email)).thenReturn(Optional.of(adminUser));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        // Removed: verify(userRepository, times(1)).findByEmail(email);
    }

    /**
     * Tests doFilterInternal with a valid JWT token, expecting authentication to be set.
     * This test replaces the previous "UserNotFoundInRepo" test as the filter doesn't query the repo.
     * If the token is valid, authentication will be set based on claims.
     */
    @Test
    void doFilterInternal_ValidToken_ShouldSetAuthenticationRegardlessOfDbPresence() throws Exception {
        String email = "userfromtoken@example.com";
        List<String> roles = Collections.singletonList("USER");
        String validToken = generateToken(email, roles, TimeUnit.MINUTES.toMillis(10));

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/protected-resource");
        // Removed: when(userRepository.findByEmail(email)).thenReturn(Optional.empty()); // Mock user NOT found (no longer relevant)

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Authentication should be set because the token is valid and contains a subject
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));

        // Filter chain should still continue
        verify(filterChain, times(1)).doFilter(request, response);
        // No explicit 401 from filter for this case
        verify(response, never()).setStatus(anyInt());
        // No userRepository interaction to verify
    }


    /**
     * Tests doFilterInternal with an expired JWT token.
     * Expected: Returns 401 Unauthorized and stops the filter chain.
     */
    @Test
    void doFilterInternal_ExpiredToken_ShouldReturnUnauthorized() throws Exception {
        String expiredToken = generateExpiredToken("expired@example.com", Collections.singletonList("USER"));

        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
        when(request.getRequestURI()).thenReturn("/protected-resource");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // No userRepository interaction to verify
    }

    /**
     * Tests doFilterInternal with a JWT token having an invalid signature.
     * Expected: Returns 401 Unauthorized and stops the filter chain.
     */
    @Test
    void doFilterInternal_InvalidSignature_ShouldReturnUnauthorized() throws Exception {
        String invalidSignatureToken = generateTokenWithBadSignature("badsign@example.com", Collections.singletonList("USER"), TimeUnit.MINUTES.toMillis(10));

        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidSignatureToken);
        when(request.getRequestURI()).thenReturn("/protected-resource");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // No userRepository interaction to verify
    }

    /**
     * Tests doFilterInternal with a malformed JWT token (e.g., not a valid JWS compact serialization).
     * Expected: Returns 401 Unauthorized and stops the filter chain.
     */
    @Test
    void doFilterInternal_MalformedToken_ShouldReturnUnauthorized() throws Exception {
        // A truly malformed token that Jwts.parserBuilder() cannot even attempt to parse
        String malformedToken = "Bearer this.is.not.a.valid.jwt.token.at.all";

        when(request.getHeader("Authorization")).thenReturn(malformedToken);
        when(request.getRequestURI()).thenReturn("/protected-resource");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // No userRepository interaction to verify
    }

    /**
     * Tests doFilterInternal with a JWT token that has no "roles" claim.
     * Expected: Authentication is set with an empty list of authorities.
     */
    @Test
    void doFilterInternal_TokenWithNoRoles_ShouldSetAuthenticationWithEmptyAuthorities() throws Exception {
        String email = "norole@example.com";
        // Generate a token without a "roles" claim to simulate a token that legitimately has no roles
        String tokenWithoutRoles = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)))
                .signWith(signKey, SignatureAlgorithm.HS256)
                .compact();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenWithoutRoles);
        when(request.getRequestURI()).thenReturn("/protected-resource");
        // Removed: when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty()); // Authorities should be empty
        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        // No userRepository interaction to verify
    }
}
