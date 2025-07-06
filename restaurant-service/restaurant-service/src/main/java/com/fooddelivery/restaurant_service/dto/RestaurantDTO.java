
package com.fooddelivery.restaurant_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDTO {

    private Long id; // ✅ Add ID to map from entity to DTO

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Cuisine is required")
    private String cuisine;

    private String status; // ✅ Add status for availability (OPEN, CLOSED, etc.)
}
