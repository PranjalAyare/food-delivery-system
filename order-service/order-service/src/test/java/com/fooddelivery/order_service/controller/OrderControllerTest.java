// order-service/src/test/java/com/fooddelivery/order_service/controller/OrderControllerTest.java
package com.fooddelivery.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.order_service.model.Order;
import com.fooddelivery.order_service.service.OrderService;
import com.fooddelivery.order_service.config.TestSecurityConfig;
import com.fooddelivery.order_service.security.JwtFilter;
import com.fooddelivery.order_service.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString; // Import anyString
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the OrderController layer using Spring's @WebMvcTest.
 * This slice test focuses on the web layer, mocking out service and other external dependencies.
 */
@WebMvcTest(
    controllers = OrderController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtFilter.class // Exclude our custom JwtFilter as Spring Security Test handles authentication
    )
)
@Import(TestSecurityConfig.class) // Import the test-specific security configuration
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc; // Used to perform HTTP requests in tests

    @MockBean
    private OrderService orderService; // Mock the OrderService dependency

    @MockBean // Mock the JwtUtil as it's a dependency of OrderController
    private JwtUtil jwtUtil;

    private ObjectMapper objectMapper = new ObjectMapper();
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Initialize a test order with all required fields
        testOrder = new Order(1L, 101L, 201L, 50.0, LocalDateTime.now(), "PENDING", "CREDIT_CARD");
        // Ensure ObjectMapper can handle Java 8 Date/Time types
        objectMapper.findAndRegisterModules();
    }

    // --- Test for POST /orders ---
    @Test
    @WithMockUser(username = "customer", roles = {"USER"}, authorities = {"ROLE_USER"}) // Mock authenticated user
    void testPlaceOrder_Authenticated() throws Exception {
        // Arrange
        // Input order from request (ID, status, orderTime are typically null initially)
        Order newOrder = new Order(null, 102L, 202L, 75.0, null, null, "DEBIT_CARD");
        // Expected saved order (with ID, orderTime, and initial status from service)
        Order savedOrder = new Order(2L, 102L, 202L, 75.0, LocalDateTime.now(), "PAYMENT_INITIATED", "DEBIT_CARD");

        // Mock JwtUtil to return a customer ID when extractCustomerId is called with any string
        when(jwtUtil.extractCustomerId(anyString())).thenReturn(102L);

        // Mock orderService.placeOrder to return the savedOrder
        when(orderService.placeOrder(any(Order.class))).thenReturn(savedOrder);

        // Act & Assert
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder))
                .header("Authorization", "Bearer dummy-jwt-token") // Provide a dummy token for the filter
                .with(SecurityMockMvcRequestPostProcessors.csrf())) // Add CSRF token for POST requests
            .andExpect(status().isCreated()) // Expect 201 Created status
            .andExpect(jsonPath("$.id").value(2L))
            .andExpect(jsonPath("$.customerId").value(102L))
            .andExpect(jsonPath("$.restaurantId").value(202L))
            .andExpect(jsonPath("$.totalAmount").value(75.0))
            .andExpect(jsonPath("$.status").value("PAYMENT_INITIATED"))
            .andExpect(jsonPath("$.paymentMethod").value("DEBIT_CARD"));

        // Verify that the service method was called
        verify(jwtUtil, times(1)).extractCustomerId(anyString());
        verify(orderService, times(1)).placeOrder(any(Order.class));
    }

    @Test
    void testPlaceOrder_Unauthenticated() throws Exception {
        // Arrange
        Order newOrder = new Order(null, 102L, 202L, 75.0, null, null, "DEBIT_CARD");

        // Act & Assert
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder)))
            .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized

        // Verify that no interactions occurred with the service or JwtUtil
        verifyNoInteractions(orderService);
        verifyNoInteractions(jwtUtil);
    }

    // --- Test for GET /orders ---
    @Test
    @WithMockUser(username = "customer", roles = {"USER"})
    void testGetAllOrders_Authenticated() throws Exception {
        // Arrange
        List<Order> orders = Arrays.asList(
            testOrder,
            new Order(3L, 103L, 203L, 120.0, LocalDateTime.now(), "DELIVERED", "CASH")
        );
        when(orderService.getAllOrders()).thenReturn(orders);

        // Act & Assert
        mockMvc.perform(get("/orders"))
            .andExpect(status().isOk()) // Expect 200 OK
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[1].customerId").value(103L));

        // Verify service method call
        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    void testGetAllOrders_Unauthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/orders"))
            .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized

        // Verify no service interaction
        verifyNoInteractions(orderService);
    }

    // --- Test for GET /orders/{id} ---
    @Test
    @WithMockUser(username = "customer", roles = {"USER"})
    void testGetOrderById_Found_Authenticated() throws Exception {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        mockMvc.perform(get("/orders/1"))
            .andExpect(status().isOk()) // Expect 200 OK
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.customerId").value(101L));

        // Verify service method call
        verify(orderService, times(1)).getOrderById(1L);
    }

    @Test
    @WithMockUser(username = "customer", roles = {"USER"})
    void testGetOrderById_NotFound_Authenticated() throws Exception {
        // Arrange
        when(orderService.getOrderById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/orders/99"))
            .andExpect(status().isNotFound()); // Expect 404 Not Found

        // Verify service method call
        verify(orderService, times(1)).getOrderById(99L);
    }

    @Test
    void testGetOrderById_Unauthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/orders/1"))
            .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized

        // Verify no service interaction
        verifyNoInteractions(orderService);
    }

    // --- Test for PUT /orders/{id} ---
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testUpdateOrder_Found_Authenticated() throws Exception {
        // Arrange
        Order updatedDetails = new Order(1L, 101L, 201L, 60.0, testOrder.getOrderTime(), "DELIVERED", "PAYPAL");
        when(orderService.updateOrder(eq(1L), any(Order.class))).thenReturn(Optional.of(updatedDetails));

        // Act & Assert
        mockMvc.perform(put("/orders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDetails))
                .with(SecurityMockMvcRequestPostProcessors.csrf())) // Add CSRF token for PUT requests
            .andExpect(status().isOk()) // Expect 200 OK
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.totalAmount").value(60.0))
            .andExpect(jsonPath("$.status").value("DELIVERED"))
            .andExpect(jsonPath("$.paymentMethod").value("PAYPAL"));

        // Verify service method call
        verify(orderService, times(1)).updateOrder(eq(1L), any(Order.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testUpdateOrder_NotFound_Authenticated() throws Exception {
        // Arrange
        Order updatedDetails = new Order(99L, 101L, 201L, 60.0, LocalDateTime.now(), "DELIVERED", "PAYPAL");
        when(orderService.updateOrder(eq(99L), any(Order.class))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(put("/orders/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDetails))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isNotFound()); // Expect 404 Not Found

        // Verify service method call
        verify(orderService, times(1)).updateOrder(eq(99L), any(Order.class));
    }

    @Test
    void testUpdateOrder_Unauthenticated() throws Exception {
        // Arrange
        Order updatedDetails = new Order(1L, 101L, 201L, 60.0, LocalDateTime.now(), "DELIVERED", "PAYPAL");

        // Act & Assert
        mockMvc.perform(put("/orders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDetails)))
            .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized

        // Verify no service interaction
        verifyNoInteractions(orderService);
    }


    // --- Test for DELETE /orders/{id} ---
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteOrder_Authenticated() throws Exception {
        // Arrange
        doNothing().when(orderService).deleteOrder(1L);

        // Act & Assert
        mockMvc.perform(delete("/orders/1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isNoContent()); // Expect 204 No Content

        // Verify service method call
        verify(orderService, times(1)).deleteOrder(1L);
    }

    @Test
    void testDeleteOrder_Unauthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/orders/1"))
            .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized

        // Verify no service interaction
        verifyNoInteractions(orderService);
    }

    // --- Test for PUT /orders/{id}/status ---
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testUpdateOrderStatus_Authenticated() throws Exception {
        // Arrange
        String newStatus = "DELIVERED";
        Order updatedOrder = new Order(1L, testOrder.getCustomerId(), testOrder.getRestaurantId(),
                                       testOrder.getTotalAmount(), testOrder.getOrderTime(), newStatus, testOrder.getPaymentMethod());
        when(orderService.updateOrderStatus(eq(1L), eq(newStatus))).thenReturn(Optional.of(updatedOrder));

        // Act & Assert
        mockMvc.perform(put("/orders/1/status")
                .param("status", newStatus)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(newStatus));

        verify(orderService, times(1)).updateOrderStatus(eq(1L), eq(newStatus));
    }

    @Test
    void testUpdateOrderStatus_Unauthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/orders/1/status")
                .param("status", "DELIVERED"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }
}
