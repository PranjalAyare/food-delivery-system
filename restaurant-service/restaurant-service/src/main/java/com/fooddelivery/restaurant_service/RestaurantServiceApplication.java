
package com.fooddelivery.restaurant_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient; // Enable service discovery
import org.springframework.scheduling.annotation.EnableScheduling;       // Enable scheduled tasks for monitoring

@SpringBootApplication
@EnableDiscoveryClient // Enables this service to register with Eureka
@EnableScheduling      // Enables Spring's scheduled task execution (for example, for MonitoringService)
public class RestaurantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantServiceApplication.class, args);
    }
}
