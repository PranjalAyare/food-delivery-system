package com.fooddelivery.order_service.feign;

import com.fooddelivery.order_service.dto.RestaurantDto; // Import the DTO you created in Step 1.2
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// @FeignClient annotation tells Spring Cloud that this is a Feign client.
// 'name': Must match the 'service-id' of your restaurant-service in Eureka (e.g., 'restaurant-service').
// 'configuration': We'll create this class in Step 5 to handle JWT forwarding.
@FeignClient(name = "restaurant-service", configuration = FeignClientConfig.class)
public interface RestaurantServiceClient {

    /**
     * Defines a method to get restaurant details by its ID.
     * This method corresponds to a GET endpoint in your restaurant-service.
     * For example, if restaurant-service has:
     * @GetMapping("/restaurants/{id}")
     * public ResponseEntity<RestaurantDto> getRestaurantById(@PathVariable("id") String id) { ... }
     * Then this Feign method will call that endpoint.
     *
     * @param id The ID of the restaurant to retrieve.
     * @return RestaurantDto containing the restaurant's details.
     */
    @GetMapping("/restaurants/{id}")
    RestaurantDto getRestaurantById(@PathVariable("id") Long id); // Ensure ID type matches your Restaurant model (Long or String)
}