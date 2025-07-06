package com.fooddelivery.order_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil {

    // Use the same secret as your auth-service (should match!)
    private final String SECRET_KEY ="super-secret-key-for-jwt-signing-must-be-strong-12345"; // 32+ chars

    public Long extractCustomerId(String token) {
        Claims claims = extractAllClaims(token);
        return Long.valueOf(claims.get("userId").toString());
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
}
