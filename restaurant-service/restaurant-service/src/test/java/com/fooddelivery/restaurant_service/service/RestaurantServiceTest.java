
package com.fooddelivery.restaurant_service.service;

import com.fooddelivery.restaurant_service.dto.RestaurantDTO;
import com.fooddelivery.restaurant_service.exception.ResourceNotFoundException;
import com.fooddelivery.restaurant_service.model.Restaurant;
import com.fooddelivery.restaurant_service.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private Restaurant restaurant;
    private RestaurantDTO dto;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant(1L, "KFC", "Delhi", "Fast Food", "OPEN");
        dto = new RestaurantDTO(null, "KFC", "Delhi", "Fast Food", "OPEN");
    }

    @Test
    void testCreateRestaurant() {
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);
        Restaurant saved = restaurantService.createRestaurant(dto);

        assertNotNull(saved);
        assertEquals("KFC", saved.getName());
        verify(restaurantRepository).save(any(Restaurant.class));
    }

    @Test
    void testGetAllRestaurants() {
        when(restaurantRepository.findAll()).thenReturn(List.of(restaurant));
        List<Restaurant> list = restaurantService.getAllRestaurants();

        assertEquals(1, list.size());
        assertEquals("KFC", list.get(0).getName());
    }

    @Test
    void testGetRestaurantById_found() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        Restaurant result = restaurantService.getRestaurantById(1L);

        assertEquals("KFC", result.getName());
    }

    @Test
    void testGetRestaurantById_notFound() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> restaurantService.getRestaurantById(99L));
    }

    @Test
    void testUpdateRestaurant_success() {
        Restaurant updated = new Restaurant(1L, "Updated", "Mumbai", "Indian", "CLOSED");
        RestaurantDTO updateDto = new RestaurantDTO(null, "Updated", "Mumbai", "Indian", "CLOSED");

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(updated);

        Restaurant result = restaurantService.updateRestaurant(1L, updateDto);
        assertEquals("Updated", result.getName());
        assertEquals("CLOSED", result.getStatus());
    }

    @Test
    void testDeleteRestaurant_success() {
        when(restaurantRepository.existsById(1L)).thenReturn(true);
        boolean deleted = restaurantService.deleteRestaurant(1L);

        assertTrue(deleted);
        verify(restaurantRepository).deleteById(1L);
    }

    @Test
    void testDeleteRestaurant_notFound() {
        when(restaurantRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> restaurantService.deleteRestaurant(99L));
    }
}
