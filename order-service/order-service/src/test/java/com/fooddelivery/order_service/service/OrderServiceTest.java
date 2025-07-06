// order-service/src/test/java/com/fooddelivery/order_service/service/OrderServiceTest.java
package com.fooddelivery.order_service.service;

import com.fooddelivery.order_service.dto.PaymentRequest;
import com.fooddelivery.order_service.dto.RestaurantDto;
import com.fooddelivery.order_service.feign.PaymentServiceClient;
import com.fooddelivery.order_service.feign.RestaurantServiceClient;
import com.fooddelivery.order_service.model.Order;
import com.fooddelivery.order_service.repository.OrderRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the OrderService class.
 * Uses Mockito to mock dependencies and verify interactions.
 * Applies lenient strictness to avoid UnnecessaryStubbingException.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Use lenient strictness for tests in this class
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    @InjectMocks
    private OrderService orderService; // Injects mocks into orderService

    /**
     * Tests the scenario where an order is placed successfully,
     * including successful restaurant validation and payment initiation.
     */
    @Test
    @DisplayName("✅ Place order with successful payment")
    void testPlaceOrder_SuccessfulPayment() {
        // Arrange
        Long customerId = 10L;
        Long restaurantId = 1L;
        Double totalAmount = 299.99;
        String paymentMethod = "CARD";
        Long orderId = 100L;

        Order initialOrder = new Order();
        initialOrder.setCustomerId(customerId);
        initialOrder.setRestaurantId(restaurantId);
        initialOrder.setTotalAmount(totalAmount);
        initialOrder.setPaymentMethod(paymentMethod);
        initialOrder.setStatus(null); // Ensure initial status is null as service sets it
        initialOrder.setOrderTime(null); // Ensure initial time is null as service sets it

        // Mock a valid and open restaurant response
        RestaurantDto restaurantDto = new RestaurantDto();
        restaurantDto.setId(restaurantId);
        restaurantDto.setName("Testaurant");
        restaurantDto.setStatus("OPEN");
        when(restaurantServiceClient.getRestaurantById(restaurantId)).thenReturn(restaurantDto);

        // First save: simulate database assigning an ID and initial status (PENDING)
        // Added null check for 'order' within argThat predicate to prevent NPE during matching
        when(orderRepository.save(argThat(order -> order != null && (order.getStatus() == null || "PENDING".equals(order.getStatus())))))
                .thenAnswer(invocation -> {
                    Order saved = invocation.getArgument(0);
                    saved.setId(orderId);
                    saved.setOrderTime(LocalDateTime.now());
                    saved.setStatus("PENDING");
                    return saved;
                });

        // Mock successful payment initiation response
        when(paymentServiceClient.processPayment(any(PaymentRequest.class)))
                .thenReturn(new ResponseEntity<>("Payment initiated successfully", HttpStatus.OK));

        // Second save: simulate database saving the order with PAYMENT_INITIATED status
        // Added null check for 'order' within argThat predicate
        when(orderRepository.save(argThat(order -> order != null && "PAYMENT_INITIATED".equals(order.getStatus()))))
                .thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        Order result = orderService.placeOrder(initialOrder);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getId(), "Order ID should be assigned");
        assertEquals(customerId, result.getCustomerId(), "Customer ID should be preserved");
        assertEquals(restaurantId, result.getRestaurantId(), "Restaurant ID should be preserved");
        assertEquals(totalAmount, result.getTotalAmount(), 0.001, "Total amount should be preserved");
        assertNotNull(result.getOrderTime(), "Order time should be set");
        assertEquals("PAYMENT_INITIATED", result.getStatus(), "Order status should be PAYMENT_INITIATED after successful payment");
        assertEquals(paymentMethod, result.getPaymentMethod(), "Payment method should be preserved");

        // Verify interactions
        verify(restaurantServiceClient, times(1)).getRestaurantById(restaurantId); // Restaurant validated
        verify(orderRepository, times(2)).save(any(Order.class)); // Called twice: initial save and status update
        verify(paymentServiceClient, times(1)).processPayment(any(PaymentRequest.class)); // Payment initiated
    }

    /**
     * Tests the scenario where an order is placed for an invalid/non-existent restaurant.
     * Expects an IllegalArgumentException and no order persistence.
     */
    @Test
    @DisplayName("❌ Should throw IllegalArgumentException for invalid restaurant")
    void testPlaceOrder_InvalidRestaurant() {
        // Arrange
        Order order = new Order();
        order.setRestaurantId(99L); // Non-existent restaurant ID
        order.setTotalAmount(199.99);
        order.setPaymentMethod("UPI");
        order.setCustomerId(20L);

        // Mock the restaurant service to return null, indicating restaurant not found
        when(restaurantServiceClient.getRestaurantById(99L)).thenReturn(null);

        // Act & Assert
        // Expect an IllegalArgumentException to be thrown
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.placeOrder(order);
        });

        // Verify the exception message matches the service's message
        assertEquals("Restaurant not found.", exception.getMessage());

        // Verify that orderRepository.save was NOT called
        verify(orderRepository, never()).save(any(Order.class));
        // Verify that paymentServiceClient.processPayment was NOT called
        verify(paymentServiceClient, never()).processPayment(any(PaymentRequest.class));
        verify(restaurantServiceClient, times(1)).getRestaurantById(99L); // Restaurant validation attempted
    }

    /**
     * Tests the scenario where an order is placed for a closed restaurant.
     * Expects an IllegalArgumentException and no order persistence.
     */
    @Test
    @DisplayName("❌ Should throw IllegalArgumentException for closed restaurant")
    void testPlaceOrder_ClosedRestaurant() {
        // Arrange
        Order order = new Order();
        order.setRestaurantId(1L);
        order.setTotalAmount(199.99);
        order.setPaymentMethod("UPI");
        order.setCustomerId(20L);

        // Mock a valid but closed restaurant response
        RestaurantDto restaurantDto = new RestaurantDto();
        restaurantDto.setId(1L);
        restaurantDto.setName("Closed Eats");
        restaurantDto.setStatus("CLOSED"); // Set status to CLOSED
        when(restaurantServiceClient.getRestaurantById(1L)).thenReturn(restaurantDto);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.placeOrder(order);
        });

        // Verify the exception message
        assertEquals("Restaurant is currently closed or unavailable.", exception.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentServiceClient, never()).processPayment(any(PaymentRequest.class));
        verify(restaurantServiceClient, times(1)).getRestaurantById(1L);
    }

    /**
     * Tests the scenario where payment initiation fails for an order.
     * Expects the order status to be "PAYMENT_ERROR".
     */
    @Test
    @DisplayName("❌ Should mark status as PAYMENT_ERROR if payment fails")
    void testPlaceOrder_PaymentFails() {
        // Arrange
        Long customerId = 11L;
        Long restaurantId = 1L;
        Double totalAmount = 399.99;
        String paymentMethod = "UPI";
        Long orderId = 200L;

        Order initialOrder = new Order();
        initialOrder.setCustomerId(customerId);
        initialOrder.setRestaurantId(restaurantId);
        initialOrder.setTotalAmount(totalAmount);
        initialOrder.setPaymentMethod(paymentMethod);
        initialOrder.setStatus(null); // Ensure initial status is null
        initialOrder.setOrderTime(null); // Ensure initial time is null

        // Mock a valid and open restaurant
        RestaurantDto restaurantDto = new RestaurantDto();
        restaurantDto.setId(restaurantId);
        restaurantDto.setName("Testaurant");
        restaurantDto.setStatus("OPEN");
        when(restaurantServiceClient.getRestaurantById(restaurantId)).thenReturn(restaurantDto);

        // First save: simulate database assigning an ID and initial status (PENDING)
        // Added null check for 'order' within argThat predicate
        when(orderRepository.save(argThat(order -> order != null && (order.getStatus() == null || "PENDING".equals(order.getStatus())))))
                .thenAnswer(invocation -> {
                    Order saved = invocation.getArgument(0);
                    saved.setId(orderId);
                    saved.setOrderTime(LocalDateTime.now());
                    saved.setStatus("PENDING");
                    return saved;
                });

        // Mock payment service to throw a RuntimeException, simulating a failure
        when(paymentServiceClient.processPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Simulated Payment Service Failure"));

        // Second save: simulate database saving the order with PAYMENT_ERROR status
        // Added null check for 'order' within argThat predicate
        when(orderRepository.save(argThat(order -> order != null && "PAYMENT_ERROR".equals(order.getStatus()))))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.placeOrder(initialOrder);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getId(), "Order ID should be assigned");
        assertEquals("PAYMENT_ERROR", result.getStatus(), "Order status should be PAYMENT_ERROR after payment failure");

        // Verify interactions
        verify(restaurantServiceClient, times(1)).getRestaurantById(restaurantId);
        verify(orderRepository, times(2)).save(any(Order.class)); // Called twice: initial save and status update to PAYMENT_ERROR
        verify(paymentServiceClient, times(1)).processPayment(any(PaymentRequest.class));
    }


    /**
     * Tests retrieving an order by its ID when it exists.
     */
    @Test
    @DisplayName("🔍 Get order by ID - Found")
    void testGetOrderById_Found() {
        // Arrange
        Order order = new Order();
        order.setId(123L);
        order.setStatus("DELIVERED");
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order));

        // Act
        Optional<Order> result = orderService.getOrderById(123L);

        // Assert
        assertTrue(result.isPresent(), "Order should be found");
        assertEquals(123L, result.get().getId(), "Retrieved order ID should match");
        assertEquals("DELIVERED", result.get().getStatus(), "Retrieved order status should match");
        verify(orderRepository, times(1)).findById(123L);
    }

    /**
     * Tests retrieving an order by its ID when it does not exist.
     */
    @Test
    @DisplayName("🔍 Get order by ID - Not Found")
    void testGetOrderById_NotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Order> result = orderService.getOrderById(999L);

        // Assert
        assertFalse(result.isPresent(), "Order should not be found");
        verify(orderRepository, times(1)).findById(999L);
    }

    /**
     * Tests updating an existing order's status.
     */
    @Test
    @DisplayName("🔁 Should update order status when order exists")
    void testUpdateOrderStatus() {
        // Arrange
        Order existing = new Order();
        existing.setId(999L);
        existing.setCustomerId(1L);
        existing.setRestaurantId(1L);
        existing.setTotalAmount(100.0);
        existing.setOrderTime(LocalDateTime.now());
        existing.setStatus("PENDING");
        existing.setPaymentMethod("CARD");

        when(orderRepository.findById(999L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<Order> updatedOptional = orderService.updateOrderStatus(999L, "DELIVERED");

        // Assert
        assertTrue(updatedOptional.isPresent(), "Updated order should be present");
        assertEquals("DELIVERED", updatedOptional.get().getStatus(), "Order status should be updated to DELIVERED");
        verify(orderRepository, times(1)).findById(999L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    /**
     * Tests updating an existing order's status when the order does not exist.
     */
    @Test
    @DisplayName("❌ Should not update order status when order does not exist")
    void testUpdateOrderStatus_NotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Order> updatedOptional = orderService.updateOrderStatus(999L, "DELIVERED");

        // Assert
        assertFalse(updatedOptional.isPresent(), "Updated order should not be present if original not found");
        verify(orderRepository, times(1)).findById(999L);
        verify(orderRepository, never()).save(any(Order.class)); // Save should not be called
    }

    /**
     * Tests updating an entire order object.
     */
    @Test
    @DisplayName("🔄 Update entire order when order exists")
    void testUpdateOrder_Found() {
        // Arrange
        Order existing = new Order();
        existing.setId(1L);
        existing.setCustomerId(101L);
        existing.setRestaurantId(201L);
        existing.setTotalAmount(50.0);
        existing.setOrderTime(LocalDateTime.of(2023, 1, 1, 10, 0));
        existing.setStatus("PENDING");
        existing.setPaymentMethod("CREDIT_CARD");

        Order updatedDetails = new Order(1L, 101L, 201L, 60.0, existing.getOrderTime(), "DELIVERED", "PAYPAL");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the argument passed

        // Act
        Optional<Order> result = orderService.updateOrder(1L, updatedDetails);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(60.0, result.get().getTotalAmount());
        assertEquals("DELIVERED", result.get().getStatus());
        assertEquals("PAYPAL", result.get().getPaymentMethod());

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    /**
     * Tests updating an entire order object when it does not exist.
     */
    @Test
    @DisplayName("❌ Not update order when order does not exist")
    void testUpdateOrder_NotFound() {
        // Arrange
        Order updatedDetails = new Order(99L, 101L, 201L, 60.0, LocalDateTime.now(), "DELIVERED", "PAYPAL");
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Optional<Order> result = orderService.updateOrder(99L, updatedDetails);

        // Assert
        assertFalse(result.isPresent());
        verify(orderRepository, times(1)).findById(99L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    /**
     * Tests deleting an order by ID.
     */
    @Test
    @DisplayName("🗑️ Delete order by ID")
    void testDeleteOrder() {
        // Arrange
        doNothing().when(orderRepository).deleteById(1L);

        // Act
        orderService.deleteOrder(1L);

        // Assert
        verify(orderRepository, times(1)).deleteById(1L);

        
    }
}
