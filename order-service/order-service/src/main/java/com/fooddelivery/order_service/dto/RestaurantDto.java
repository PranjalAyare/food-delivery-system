package com.fooddelivery.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Lombok: Generates a no-argument constructor
@AllArgsConstructor // Lombok: Generates a constructor with all fields
public class RestaurantDto {

    private Long id; // Corresponds to the ID in the Restaurant model
    private String name;
    private String location;
    private String cuisine;
    private String status; // Add this for future availability checks (e.g., "OPEN", "CLOSED")

    // Note: If your RestaurantService's GET /restaurants/{id} endpoint
    // returns the full Restaurant model, or a DTO like this, this will match.
    // If it returns a different structure, you'd adjust this DTO accordingly.
}