package com.fooddelivery.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient; // Import this for Eureka
import org.springframework.cloud.openfeign.EnableFeignClients; // Import this for Feign Client

@SpringBootApplication
@EnableDiscoveryClient // Enables this service to register with Eureka and discover others
@EnableFeignClients // Adds this annotation to enable Feign client scanning
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
