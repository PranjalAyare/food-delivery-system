
// payment-service/src/main/java/com/fooddelivery/payment_service/PaymentServiceApplication.java
package com.fooddelivery.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient // Enables this service to register with Eureka
@EnableScheduling      // Enables Spring's scheduled task execution (for monitoring)
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}
