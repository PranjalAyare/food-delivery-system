
package com.fooddelivery.restaurant_service.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    // Base URL of the restaurant service
    private final String BASE_URL = "http://localhost:8082/restaurants";

    // JWT token for monitoring authentication
    @Value("${monitoring.jwt.token:dummy-admin-jwt-token-for-monitoring}")
    private String monitoringJwtToken;

    /**
     * Periodically performs a health check by calling GET /restaurants.
     * This method runs every 30 seconds without modifying any data.
     */
    @PostConstruct
    @Async
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void monitorRestaurantEndpoints() {
        log.info("\uD83D\uDD01 [Monitor] Starting restaurant service health check...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(monitoringJwtToken); // Attach bearer token

        try {
            // Perform GET /restaurants
            log.info("\uD83D\uDD01 [Monitor] Sending GET /restaurants");
            HttpEntity<String> getRequest = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(BASE_URL, HttpMethod.GET, getRequest, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [GET /restaurants] Success. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            } else {
                log.warn("⚠️ [GET /restaurants] Failed. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            }

        } catch (HttpClientErrorException e) {
            log.error("❌ [Monitor] HTTP Error (Status: {}, Body: {}): {}", e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ [Monitor] Unexpected Error: {}", e.getMessage(), e);
        }

        // Optional delay between executions (already handled by @Scheduled, but safe to keep)
        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("[Monitor] Monitoring thread interrupted.");
        }
    }
}
