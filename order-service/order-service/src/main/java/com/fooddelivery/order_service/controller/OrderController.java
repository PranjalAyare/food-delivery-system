

package com.fooddelivery.order_service.controller;

import com.fooddelivery.order_service.model.Order;
import com.fooddelivery.order_service.security.JwtUtil;
import com.fooddelivery.order_service.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    @Autowired
    public OrderController(OrderService orderService, JwtUtil jwtUtil) {
        this.orderService = orderService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody Order order, HttpServletRequest request) {
        try {
            // ✅ Extract JWT token from header
            String tokenHeader = request.getHeader("Authorization");
            if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header.");
            }

            String token = tokenHeader.substring(7); // Remove "Bearer "
            Long customerId = jwtUtil.extractCustomerId(token);
            order.setCustomerId(customerId); // ✅ Inject extracted customer ID

            // ✅ Validate required fields
            if (order.getRestaurantId() == null || order.getTotalAmount() == null || order.getPaymentMethod() == null) {
                return ResponseEntity.badRequest().body("Missing required fields: restaurantId, totalAmount, or paymentMethod.");
            }

            Order savedOrder = orderService.placeOrder(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error placing order: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        return orderService.updateOrder(id, order)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ New: Update only status
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        return orderService.updateOrderStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

