
// order-service/src/main/java/com/fooddelivery/order_service/service/OrderService.java
package com.fooddelivery.order_service.service;

import com.fooddelivery.order_service.dto.PaymentRequest;
import com.fooddelivery.order_service.dto.RestaurantDto;
import com.fooddelivery.order_service.feign.PaymentServiceClient;
import com.fooddelivery.order_service.feign.RestaurantServiceClient;
import com.fooddelivery.order_service.model.Order;
import com.fooddelivery.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for handling order-related business logic.
 * Manages operations such as placing orders, retrieving, updating, and deleting orders.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;

    /**
     * Constructor for OrderService, using constructor injection for all dependencies.
     * This is the recommended practice for dependency injection in Spring.
     *
     * @param orderRepository The repository for Order entities.
     * @param paymentServiceClient Feign client for interacting with the payment service.
     * @param restaurantServiceClient Feign client for interacting with the restaurant service.
     */
    @Autowired
    public OrderService(OrderRepository orderRepository,
                        PaymentServiceClient paymentServiceClient,
                        RestaurantServiceClient restaurantServiceClient) {
        this.orderRepository = orderRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.restaurantServiceClient = restaurantServiceClient;
    }

    /**
     * Places a new order. This is a transactional operation.
     * Steps:
     * 1. Validates the existence and status of the restaurant using the Feign client.
     * 2. Sets the initial status of the order to "PENDING" and records the order time.
     * 3. Saves the order to the database.
     * 4. Initiates a payment process by calling the Payment Service via Feign.
     * 5. Updates the order status based on the payment service response.
     *
     * @param order The Order object to be placed.
     * @return The saved and updated Order object.
     * @throws IllegalArgumentException if the restaurant is not found or is closed.
     * @throws RuntimeException if there's an issue with payment (e.g., Feign client exception).
     */
    @Transactional
    public Order placeOrder(Order order) {
        log.info("Attempting to place order for customerId: {}", order.getCustomerId());

        // Step 1: Validate restaurant existence and status
        log.info("Validating restaurant with ID: {}", order.getRestaurantId());
        RestaurantDto restaurant = restaurantServiceClient.getRestaurantById(order.getRestaurantId());

        if (restaurant == null) {
            log.error("Restaurant with ID {} not found.", order.getRestaurantId());
            throw new IllegalArgumentException("Restaurant not found.");
        }
        if (!"ACTIVE".equalsIgnoreCase(restaurant.getStatus())) {
            log.error("Restaurant {} (ID: {}) is not open. Current status: {}", restaurant.getName(), restaurant.getId(), restaurant.getStatus());
            throw new IllegalArgumentException("Restaurant is currently closed or unavailable.");
        }
        log.info("Restaurant {} (ID: {}) is open.", restaurant.getName(), restaurant.getId());

        // Step 2: Set initial order status and timestamp
        order.setStatus("PENDING");
        order.setOrderTime(LocalDateTime.now());

        // Step 3: Save the order initially
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved initially with ID: {} and status: {}", savedOrder.getId(), savedOrder.getStatus());

        try {
            // Step 4: Initiate payment
            log.info("Initiating payment for Order ID: {} with amount: {} via Payment Service using method: {}.",
                    savedOrder.getId(), savedOrder.getTotalAmount(), savedOrder.getPaymentMethod());

            PaymentRequest paymentRequest = new PaymentRequest(
                savedOrder.getId(),
                savedOrder.getTotalAmount(),
                savedOrder.getPaymentMethod()
            );

            ResponseEntity<String> paymentResponse = paymentServiceClient.processPayment(paymentRequest);

            // Step 5: Update order status based on payment response
            if (paymentResponse.getStatusCode().is2xxSuccessful()) {
                log.info("Payment initiated successfully for Order ID: {}. Payment Service Response: {}",
                        savedOrder.getId(), paymentResponse.getBody());
                savedOrder.setStatus("PAYMENT_INITIATED");
            } else {
                log.warn("Failed to initiate payment for Order ID: {}. Payment Service Status: {}, Body: {}",
                        savedOrder.getId(), paymentResponse.getStatusCode(), paymentResponse.getBody());
                savedOrder.setStatus("PAYMENT_FAILED");
            }
        } catch (Exception e) {
            // Catch any exceptions during payment service call (e.g., FeignClientException, network issues)
            log.error("Error calling Payment Service for Order ID {}: {}", savedOrder.getId(), e.getMessage(), e);
            savedOrder.setStatus("PAYMENT_ERROR");
        }

        // Save the order with the updated status (after payment attempt)
        return orderRepository.save(savedOrder);
    }

    /**
     * Retrieves all orders from the database.
     * @return A list of all orders.
     */
    public List<Order> getAllOrders() {
        log.info("Fetching all orders.");
        return orderRepository.findAll();
    }

    /**
     * Retrieves an order by its unique ID.
     * @param id The ID of the order to retrieve.
     * @return An Optional containing the Order if found, or an empty Optional if not found.
     */
    public Optional<Order> getOrderById(Long id) {
        log.info("Fetching order with ID: {}", id);
        return orderRepository.findById(id);
    }

    /**
     * Updates an existing order.
     * @param id The ID of the order to update.
     * @param updatedOrder The Order object containing the updated details.
     * @return An Optional containing the updated Order if found, or an empty Optional if not found.
     */
    public Optional<Order> updateOrder(Long id, Order updatedOrder) {
        log.info("Attempting to update order with ID: {}", id);
        return orderRepository.findById(id).map(existingOrder -> {
            // Update fields from the provided updatedOrder
            existingOrder.setCustomerId(updatedOrder.getCustomerId());
            existingOrder.setRestaurantId(updatedOrder.getRestaurantId());
            existingOrder.setTotalAmount(updatedOrder.getTotalAmount());
            existingOrder.setStatus(updatedOrder.getStatus());
            existingOrder.setPaymentMethod(updatedOrder.getPaymentMethod());
            // orderTime is marked as updatable=false in entity, so it won't be explicitly set here.
            Order saved = orderRepository.save(existingOrder);
            log.info("Order with ID: {} updated successfully.", saved.getId());
            return saved;
        });
    }

    /**
     * Deletes an order by its ID.
     * @param id The ID of the order to delete.
     */
    public void deleteOrder(Long id) {
        log.info("Attempting to delete order with ID: {}", id);
        orderRepository.deleteById(id);
        log.info("Order with ID: {} deleted.", id);
    }

    /**
     * Updates only the status of an existing order.
     * @param id The ID of the order to update.
     * @param newStatus The new status string.
     * @return An Optional containing the updated Order if found, or an empty Optional if not found.
     */
    public Optional<Order> updateOrderStatus(Long id, String newStatus) {
        log.info("Attempting to update status for order ID: {} to {}", id, newStatus);
        return orderRepository.findById(id).map(order -> {
            order.setStatus(newStatus);
            Order saved = orderRepository.save(order);
            log.info("Status for order ID: {} updated to {}", saved.getId(), saved.getStatus());
            return saved;
        });
    }
}

