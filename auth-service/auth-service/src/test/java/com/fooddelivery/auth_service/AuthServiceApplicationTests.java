
package com.fooddelivery.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource; // Import this

@SpringBootTest
@TestPropertySource(properties = {
    // Disable scheduling for tests to prevent AuthMonitoringService from running
    "spring.task.scheduling.enabled=false",
    // You can also explicitly disable the monitoring service if it has a @ConditionalOnProperty
    // "auth.monitoring.enabled=false" // Example, if you added a property to control it
})
class AuthServiceApplicationTests {
    @Test
    void contextLoads() {
        // This test simply ensures that the Spring Application Context loads successfully.
        // With the properties above, AuthMonitoringService's scheduled tasks won't interfere.
    }
}
