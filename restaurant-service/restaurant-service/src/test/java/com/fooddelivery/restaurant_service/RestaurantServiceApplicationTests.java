
package com.fooddelivery.restaurant_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource; // Import TestPropertySource

@SpringBootTest
@TestPropertySource(properties = {
    // Disable Eureka client for faster and isolated tests
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    // Disable scheduled tasks like the MonitoringService during tests
    "spring.task.scheduling.enabled=false"
})
class RestaurantServiceApplicationTests {

    @Test
    void contextLoads() {
        // This test simply ensures that the Spring Application Context loads successfully.
    }

}
