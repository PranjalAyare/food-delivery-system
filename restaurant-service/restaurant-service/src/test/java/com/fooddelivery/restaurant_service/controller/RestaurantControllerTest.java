
package com.fooddelivery.restaurant_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.restaurant_service.dto.RestaurantDTO;
import com.fooddelivery.restaurant_service.model.Restaurant;
import com.fooddelivery.restaurant_service.service.RestaurantService;
import com.fooddelivery.restaurant_service.exception.ResourceNotFoundException;
import com.fooddelivery.restaurant_service.config.TestSecurityConfig;
import com.fooddelivery.restaurant_service.security.JwtFilter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = RestaurantController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtFilter.class
    )
)
@Import(TestSecurityConfig.class)
public class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantService restaurantService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllRestaurants_Authenticated() throws Exception {
        Restaurant r1 = new Restaurant(1L, "KFC", "Delhi", "Fast Food", "OPEN");
        Restaurant r2 = new Restaurant(2L, "Domino's", "Mumbai", "Pizza", "CLOSED");

        when(restaurantService.getAllRestaurants()).thenReturn(Arrays.asList(r1, r2));

        mockMvc.perform(get("/restaurants")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("KFC"))
                .andExpect(jsonPath("$[1].name").value("Domino's"));
    }

    @Test
    public void testCreateRestaurant_AsAdmin() throws Exception {
        RestaurantDTO dto = new RestaurantDTO(null, "Burger King", "Bangalore", "Burgers", "OPEN");
        Restaurant restaurant = new Restaurant(3L, "Burger King", "Bangalore", "Burgers", "OPEN");

        when(restaurantService.createRestaurant(any(RestaurantDTO.class))).thenReturn(restaurant);

        mockMvc.perform(post("/restaurants")
                        .with(user("adminuser").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Burger King"));

        verify(restaurantService).createRestaurant(any(RestaurantDTO.class));
    }

    @Test
    public void testGetRestaurantById_Found_Authenticated() throws Exception {
        Restaurant restaurant = new Restaurant(4L, "Subway", "Pune", "Healthy", "OPEN");

        when(restaurantService.getRestaurantById(4L)).thenReturn(restaurant);

        mockMvc.perform(get("/restaurants/4")
                        .with(user("anyuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Subway"));
    }

    @Test
    public void testGetRestaurantById_NotFound() throws Exception {
        when(restaurantService.getRestaurantById(100L))
                .thenThrow(new ResourceNotFoundException("Restaurant not found with id: 100"));

        mockMvc.perform(get("/restaurants/100")
                        .with(user("anyuser").roles("USER")))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Restaurant not found with id: 100"));
    }

    @Test
    public void testUpdateRestaurant_AsAdmin() throws Exception {
        RestaurantDTO dto = new RestaurantDTO(null, "Pizza Hut", "Hyderabad", "Pizza", "OPEN");
        Restaurant updated = new Restaurant(5L, "Pizza Hut", "Hyderabad", "Pizza", "OPEN");

        when(restaurantService.updateRestaurant(eq(5L), any(RestaurantDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/restaurants/5")
                        .with(user("adminuser").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Pizza Hut"));
    }

    @Test
    public void testDeleteRestaurant_AsAdmin() throws Exception {
        when(restaurantService.deleteRestaurant(6L)).thenReturn(true);

        mockMvc.perform(delete("/restaurants/6")
                        .with(user("adminuser").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteRestaurant_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Restaurant not found with id: 300"))
                .when(restaurantService).deleteRestaurant(300L);

        mockMvc.perform(delete("/restaurants/300")
                        .with(user("adminuser").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Restaurant not found with id: 300"));
    }
}
