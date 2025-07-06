
package com.fooddelivery.auth_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Auth Service API",
        version = "1.0.0",
        description = "API documentation for the Food Delivery Auth Service. " +
                    "Use /auth/login to get a JWT token, then click the 'Authorize' button " +
                    "and enter it in the format: 'Bearer YOUR_TOKEN_HERE'",
        contact = @Contact(name = "Your Team", email = "contact@example.com"),
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")
    ),
    servers = {
        @Server(url = "/", description = "Default Server URL (e.g., http://localhost:8081)"),
        // You can add more server URLs if your service is deployed differently
    }
)
@SecurityScheme(
    name = "BearerAuth", // This name will be referenced by @SecurityRequirement
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "Enter JWT Bearer token **_only_** (e.g., 'Bearer abc.xyz.123')"
)
public class OpenApiConfig {
    // This class primarily uses annotations for configuration.
    // No explicit beans are typically needed here for basic OpenAPI setup.
}