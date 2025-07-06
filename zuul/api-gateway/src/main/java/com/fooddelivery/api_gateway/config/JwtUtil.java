// // zuul-gateway/src/main/java/com/fooddelivery/api_gateway/config/JwtUtil.java
// package com.fooddelivery.api_gateway.config; // Ensure this package matches your project structure

// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.security.Keys;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;

// import java.security.Key;

// @Component
// public class JwtUtil {

//     private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

//     @Value("${jwt.secret}")
//     private String secret; // This will inject the jwt.secret from application.yml

//     // This method generates the signing key from your secret string.
//     // It uses HMAC-SHA algorithms, which require a key derived from bytes.
//     private Key getSignKey() {
//         // Keys.hmacShaKeyFor expects a byte array. The secret string is converted to bytes.
//         // Ensure your 'secret' in application.yml is strong and long enough (e.g., 32 characters for HS256).
//         return Keys.hmacShaKeyFor(secret.getBytes());
//     }

//     // Extracts all claims (payload) from a given JWT token.
//     // It also implicitly validates the token's signature and checks for expiration.
//     public Claims getAllClaimsFromToken(String token) {
//         log.debug("Attempting to parse claims from token.");
//         return Jwts.parserBuilder()
//                 .setSigningKey(getSignKey()) // Use the secret key to verify the token's signature
//                 .build()
//                 .parseClaimsJws(token) // Parses the token. Throws exceptions if token is invalid/expired.
//                 .getBody(); // Returns the Claims object (the payload of the JWT)
//     }

//     // Retrieves the subject (typically the username or email) from the token.
//     public String getUsernameFromToken(String token) {
//         return getAllClaimsFromToken(token).getSubject();
//     }

//     // Retrieves the 'role' custom claim from the token.
//     public String getRoleFromToken(String token) {
//         // The .get("claimName", Class.class) method is safer for type casting.
//         return getAllClaimsFromToken(token).get("role", String.class);
//     }

//     // Retrieves the 'userId' custom claim from the token.
//     public Long getUserIdFromToken(String token) {
//         Object userIdObj = getAllClaimsFromToken(token).get("userId");
//         if (userIdObj instanceof Integer) { // JWT claims often return numbers as Integer if small enough
//             return ((Integer) userIdObj).longValue(); // Convert to Long as services expect Long IDs
//         } else if (userIdObj instanceof Long) {
//             return (Long) userIdObj;
//         }
//         log.warn("UserId claim not found or not in expected format (Integer/Long) in token.");
//         return null; // Return null or throw an exception if userId is a mandatory claim
//     }

//     // Checks if a JWT token is valid (signature correct and not expired).
//     public boolean isTokenValid(String token) {
//         try {
//             getAllClaimsFromToken(token); // If this call doesn't throw an exception, the token is valid.
//             log.debug("Token is valid.");
//             return true;
//         } catch (Exception e) {
//             // Catching a general Exception here to cover various JWT parsing/validation errors
//             // like SignatureException, ExpiredJwtException, MalformedJwtException, etc.
//             log.warn("Token validation failed in Zuul ({}): {}", e.getClass().getSimpleName(), e.getMessage());
//             return false;
//         }
//     }
// }