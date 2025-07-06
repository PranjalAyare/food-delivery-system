package com.fooddelivery.payment_service.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;
import java.util.UUID; // For generating unique data for tests

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    // The base URL for the payment service endpoints
    // For monitoring the service directly, it would be its own port (8084)
    private final String BASE_URL = "http://localhost:8084/payments";

    // This property will be injected from application.properties or application-test.properties
    // It should hold a valid JWT token obtained from your auth-service for monitoring purposes.
    @Value("${monitoring.jwt.token:dummy-jwt-token-for-monitoring}") // Default placeholder token
    private String monitoringJwtToken;

    /**
     * Initializes and schedules the payment service health checks.
     * Uses @PostConstruct to start monitoring after bean initialization,
     * and @Async to run it in a separate thread so it doesn't block startup.
     * @Scheduled ensures it runs every 30 seconds.
     */
    @PostConstruct
    @Async
    @Scheduled(fixedDelay = 30000) // Run every 30 seconds
    public void monitorPaymentEndpoints() {
        log.info("\uD83D\uDD01 [Monitor] Starting payment service health check...");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(monitoringJwtToken); // Attach the JWT token for authentication

        // Generate dynamic test data to avoid conflicts on repeated runs
        Long testOrderId = Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1000000);
        Double testAmount = 100.0 + (Math.random() * 50.0);
        String testPaymentMethod = "CREDIT_CARD";

        try {
            // 1. Test POST /payments (Process a new payment)
            log.info("\uD83D\uDD01 [Monitor] Sending POST /payments for order ID: {}", testOrderId);
            String createPaymentJson = String.format(
                    "{\"orderId\":%d, \"amount\":%.2f, \"paymentMethod\":\"%s\"}",
                    testOrderId, testAmount, testPaymentMethod);
            HttpEntity<String> createRequest = new HttpEntity<>(createPaymentJson, headers);
            ResponseEntity<String> createResponse = restTemplate.postForEntity(BASE_URL, createRequest, String.class);

            if (createResponse.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [POST /payments] Success. Status: {}, Body: {}", createResponse.getStatusCode(), createResponse.getBody());
            } else {
                log.warn("⚠️ [POST /payments] Failed with status: {}, Body: {}", createResponse.getStatusCode(), createResponse.getBody());
            }

            // 2. Test GET /payments (Get all payments)
            log.info("\uD83D\uDD01 [Monitor] Sending GET /payments");
            HttpEntity<String> getRequest = new HttpEntity<>(headers); // GET request also needs headers for security
            ResponseEntity<String> getAllResponse = restTemplate.exchange(BASE_URL, org.springframework.http.HttpMethod.GET, getRequest, String.class);

            if (getAllResponse.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [GET /payments] Success. Status: {}, Body: {}", getAllResponse.getStatusCode(), getAllResponse.getBody());
            } else {
                log.warn("⚠️ [GET /payments] Failed with status: {}, Body: {}", getAllResponse.getStatusCode(), getAllResponse.getBody());
            }

        } catch (Exception e) {
            log.error("❌ [Monitor] Error during payment service monitoring: {}", e.getMessage(), e);
        }
    }
}
