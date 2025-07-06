package com.fooddelivery.order_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "`order`") // Using backticks to escape 'order' which is a SQL reserved keyword
@Data // Lombok: Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Lombok: Generates a no-argument constructor
@AllArgsConstructor // Lombok: Generates a constructor with all fields
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for the order

    private Long customerId; // ID of the customer who placed the order
    private Long restaurantId; // ID of the restaurant for this order
    private Double totalAmount; // Total amount of the order

    private LocalDateTime orderTime; // Timestamp when the order was placed

    private String status; // Current status of the order (e.g., PENDING, PAYMENT_INITIATED, DELIVERED)

    private String paymentMethod; // New field: Method used for payment (e.g., CREDIT_CARD, UPI, WALLET)
}
