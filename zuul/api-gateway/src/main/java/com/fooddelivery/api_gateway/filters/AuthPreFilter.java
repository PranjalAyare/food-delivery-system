// // zuul-gateway/src/main/java/com/fooddelivery/api_gateway/filters/AuthPreFilter.java
// package com.fooddelivery.api_gateway.filters; // Ensure this package matches your project structure

// import com.fooddelivery.api_gateway.config.JwtUtil; // Import your JwtUtil
// import com.netflix.zuul.ZuulFilter;
// import com.netflix.zuul.context.RequestContext;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.stereotype.Component;

// import javax.servlet.http.HttpServletRequest; // IMPORTANT: This import is for Spring Boot 2.x (javax.servlet)
// import java.util.Arrays;
// import java.util.List;

// import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

// /**
//  * A Zuul pre-filter to intercept requests, validate JWTs, and inject user claims
//  * (userId, role, email) as custom headers for downstream services.
//  */
// @Component
// public class AuthPreFilter extends ZuulFilter {

//     private static final Logger log = LoggerFactory.getLogger(AuthPreFilter.class);

//     @Autowired
//     private JwtUtil jwtUtil;

//     // Define a list of paths that are considered public and do not require JWT validation.
//     // Requests to these paths will bypass this filter.
//     private static final List<String> PUBLIC_PATHS = Arrays.asList(
//             "/auth/auth/register",  // User registration
//             "/auth/auth/login",     // User login (to obtain a token)
//             "/swagger-ui.html", // Swagger UI main page
//             "/swagger-ui/",     // Swagger UI assets (e.g., /swagger-ui/index.html, /swagger-ui/swagger-ui-bundle.js)
//             "/v3/api-docs",    // OpenAPI 3.0 specification endpoint
//             "/v2/api-docs",    // Swagger 2.0 specification endpoint
//             "/actuator/"       // Spring Boot Actuator endpoints for health checks, etc.
//             // You might add other genuinely public endpoints here, e.g.:
//             // "/restaurant/all" // If you decide to make a specific endpoint to list all restaurants public
//     );

//     /**
//      * Specifies that this is a "pre" filter, meaning it runs before routing.
//      */
//     @Override
//     public String filterType() {
//         return PRE_TYPE;
//     }

//     /**
//      * Defines the order in which this filter will run among other pre-filters.
//      * Lower numbers execute earlier. This filter should run early to protect endpoints.
//      */
//     @Override
//     public int filterOrder() {
//         return 1;
//     }

//     /**
//      * Determines whether this filter should execute for the current request.
//      * It returns false for public paths, allowing them to bypass authentication.
//      */
//     @Override
//     public boolean shouldFilter() {
//         RequestContext ctx = RequestContext.getCurrentContext();
//         String requestURI = ctx.getRequest().getRequestURI();

//         // Check if the request URI starts with or equals any of the defined public paths
//         boolean isPublicPath = PUBLIC_PATHS.stream().anyMatch(path -> {
//             if (path.endsWith("/")) {
//                 // For paths ending with '/', check if requestURI starts with it (e.g., /swagger-ui/index.html)
//                 return requestURI.startsWith(path);
//             }
//             // For exact path matches (e.g., /auth/login)
//             return requestURI.equals(path);
//         });

//         if (isPublicPath) {
//             log.debug("Skipping JWT filter for public path: {}", requestURI);
//             return false; // Do not filter public paths
//         }
//         log.debug("Applying JWT filter for protected path: {}", requestURI);
//         return true; // Filter all other (protected) paths
//     }

//     /**
//      * The core logic of the filter. It extracts, validates, and processes the JWT.
//      */
//     @Override
//     public Object run() {
//         RequestContext ctx = RequestContext.getCurrentContext();
//         HttpServletRequest request = ctx.getRequest(); // Get the current HTTP request
//         String authorizationHeader = request.getHeader("Authorization"); // Get the Authorization header

//         log.debug("Processing request for: {} {}", request.getMethod(), request.getRequestURI());
//         log.debug("Authorization header status: {}", authorizationHeader != null ? "Present" : "Missing");

//         // 1. Validate the Authorization header format (must be "Bearer <token>")
//         if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
//             log.warn("Missing or invalid 'Authorization: Bearer' header for protected endpoint {}. Setting 401 Unauthorized.", request.getRequestURI());
//             ctx.setSendZuulResponse(false); // Do not forward the request to the downstream service
//             ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value()); // Set HTTP status code to 401
//             ctx.setResponseBody("Missing or invalid Authorization header"); // Set response body
//             return null; // Stop filter chain execution
//         }

//         // 2. Extract the actual JWT token by removing the "Bearer " prefix
//         String token = authorizationHeader.substring(7);

//         // 3. Validate the JWT token using JwtUtil
//         if (!jwtUtil.isTokenValid(token)) {
//             log.warn("Invalid or expired JWT token for protected endpoint {}. Setting 401 Unauthorized.", request.getRequestURI());
//             ctx.setSendZuulResponse(false);
//             ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
//             ctx.setResponseBody("Invalid or expired JWT token");
//             return null;
//         }

//         // 4. Extract claims (userId, role, email) from the valid token and add them as request headers
//         try {
//             Long userId = jwtUtil.getUserIdFromToken(token);
//             String role = jwtUtil.getRoleFromToken(token);
//             String email = jwtUtil.getUsernameFromToken(token);

//             // Ensure crucial claims are present (they should be if token is valid and issued correctly)
//             if (userId == null || role == null || email == null) {
//                 log.error("JWT token is valid but missing crucial claims (userId, role, or email) for {}. Setting 500 Internal Server Error.", request.getRequestURI());
//                 ctx.setSendZuulResponse(false);
//                 ctx.setResponseStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
//                 ctx.setResponseBody("Internal server error: Token claims missing.");
//                 return null;
//             }

//             // Add claims as custom request headers. These headers will be forwarded to the target microservice.
//             // The 'sensitive-headers: []' in application.yml ensures these are not stripped.
//             ctx.addZuulRequestHeader("X-User-ID", String.valueOf(userId));
//             ctx.addZuulRequestHeader("X-User-Role", role);
//             ctx.addZuulRequestHeader("X-User-Email", email); // X-User-Email is optional but good practice

//             log.debug("JWT validated. Added headers: X-User-ID={}, X-User-Role={}, X-User-Email={} for request to {}", userId, role, email, request.getRequestURI());

//         } catch (Exception e) {
//             // Catch any unexpected errors during claim extraction or header manipulation
//             log.error("Unexpected error processing JWT in Zuul filter for {}: {}", request.getRequestURI(), e.getMessage(), e);
//             ctx.setSendZuulResponse(false);
//             ctx.setResponseStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
//             ctx.setResponseBody("Internal server error during token processing");
//             return null;
//         }

//         return null; // If filter completes successfully, the request proceeds to the next filter/route
//     }
// }