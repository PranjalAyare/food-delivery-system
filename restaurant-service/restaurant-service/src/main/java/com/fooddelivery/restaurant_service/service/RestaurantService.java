package com.fooddelivery.restaurant_service.service;

import com.fooddelivery.restaurant_service.dto.RestaurantDTO;
import com.fooddelivery.restaurant_service.model.Restaurant;

import java.util.List;

public interface RestaurantService {
    List<Restaurant> getAllRestaurants();
    Restaurant createRestaurant(RestaurantDTO dto);
    Restaurant getRestaurantById(Long id);
    Restaurant updateRestaurant(Long id, RestaurantDTO dto);
    boolean deleteRestaurant(Long id);
}
