
package com.fooddelivery.restaurant_service.controller;

import com.fooddelivery.restaurant_service.dto.RestaurantDTO;
import com.fooddelivery.restaurant_service.model.Restaurant;
import com.fooddelivery.restaurant_service.service.RestaurantService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/restaurants")
public class RestaurantController {

    private static final Logger log = LoggerFactory.getLogger(RestaurantController.class);
    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    // --- Existing endpoints ---

    @GetMapping
    public ResponseEntity<List<Restaurant>> getAllRestaurants() {
        List<Restaurant> restaurants = restaurantService.getAllRestaurants();
        log.info("Get all restaurants API called, found {} restaurants.", restaurants.size());
        return ResponseEntity.ok(restaurants);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Restaurant> createRestaurant(@Valid @RequestBody RestaurantDTO restaurantDTO) {
        log.info("Attempting to create restaurant: {}", restaurantDTO.getName());
        Restaurant created = restaurantService.createRestaurant(restaurantDTO);
        log.info("Restaurant created successfully with ID: {}", created.getId());
        return ResponseEntity.status(201).body(created);
    }

    // 🔁 Existing endpoint: returning full entity
    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getRestaurantById(@PathVariable Long id) {
        log.info("Get restaurant by ID API called for ID: {}", id);
        Restaurant restaurant = restaurantService.getRestaurantById(id);
        log.info("Restaurant with ID {} found: {}", id, restaurant.getName());
        return ResponseEntity.ok(restaurant);
    }

    // ✅ NEW: Feign client-compatible DTO-returning endpoint
    @GetMapping("/dto/{id}")
    public ResponseEntity<RestaurantDTO> getRestaurantDtoById(@PathVariable Long id) {
        log.info("Feign-compatible DTO endpoint called for restaurant ID: {}", id);

        Restaurant restaurant = restaurantService.getRestaurantById(id); // may throw or return null
        if (restaurant == null) {
            return ResponseEntity.notFound().build();
        }

        RestaurantDTO dto = new RestaurantDTO(
            restaurant.getId(),
            restaurant.getName(),
            restaurant.getLocation(),
            restaurant.getCuisine(),
            restaurant.getStatus()
        );

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Restaurant> updateRestaurant(@PathVariable Long id, @Valid @RequestBody RestaurantDTO restaurantDTO) {
        log.info("Attempting to update restaurant with ID: {}", id);
        Restaurant updated = restaurantService.updateRestaurant(id, restaurantDTO);
        log.info("Restaurant with ID {} updated successfully.", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        log.info("Attempting to delete restaurant with ID: {}", id);
        restaurantService.deleteRestaurant(id);
        log.info("Restaurant with ID {} deleted successfully.", id);
        return ResponseEntity.noContent().build();
    }
}
