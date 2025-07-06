
package com.fooddelivery.auth_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct; // Add this import if not present, needed for @PostConstruct
import java.util.UUID;

@Service
public class AuthMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(AuthMonitoringService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private final String BASE_URL = "http://localhost:8080/auth"; // Adjust port if needed

    // We generate a unique email every time to avoid "Email already registered" error on /register
    private String generateUniqueEmail() {
        return "monitor_" + UUID.randomUUID() + "@test.com";
    }

    @PostConstruct // To ensure this runs on application startup as well, if desired
    @Scheduled(fixedDelay = 30000) // Run every 30 seconds
    public void monitorAuthEndpoints() {
        String email = generateUniqueEmail();
        String password = "MonitorPass123";
        String defaultRole = "USER"; // Default role for monitoring registration

        log.info("\uD83D\uDD01 [Monitor] Starting Auth Service health check...");

        try {
            // 1. Register dummy user
            log.info("\uD83D\uDD01 [Monitor] Sending POST /auth/register for email: {}", email);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Updated: Include 'name' and 'role' in the registration JSON
            String registerJson = String.format(
                    "{\"name\":\"Monitor User\", \"email\":\"%s\", \"password\":\"%s\", \"role\":\"%s\"}",
                    email, password, defaultRole);
            HttpEntity<String> registerRequest = new HttpEntity<>(registerJson, headers);
            ResponseEntity<String> registerResponse = restTemplate.postForEntity(BASE_URL + "/register", registerRequest, String.class);

            if (registerResponse.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [POST /auth/register] Success. Status: {}, Body: {}", registerResponse.getStatusCode(), registerResponse.getBody());
            } else {
                log.warn("⚠️ [POST /auth/register] Failed with status: {}, Body: {}", registerResponse.getStatusCode(), registerResponse.getBody());
            }


            // 2. Login dummy user
            log.info("\uD83D\uDD01 [Monitor] Sending POST /auth/login for email: {}", email);
            String loginJson = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);
            HttpEntity<String> loginRequest = new HttpEntity<>(loginJson, headers);
            ResponseEntity<String> loginResponse = restTemplate.postForEntity(BASE_URL + "/login", loginRequest, String.class);

            if (loginResponse.getStatusCode() == HttpStatus.OK) {
                log.info("✅ [POST /auth/login] Success. Token received: {}", loginResponse.getBody());
            } else {
                log.warn("⚠️ [POST /auth/login] Failed with status: {}, Body: {}", loginResponse.getStatusCode(), loginResponse.getBody());
            }

        } catch (Exception e) {
            log.error("❌ [Monitor] Error during auth monitoring: {}", e.getMessage(), e);
        }
    }
}
