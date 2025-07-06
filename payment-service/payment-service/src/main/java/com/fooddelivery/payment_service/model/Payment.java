package com.fooddelivery.payment_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments") // Using 'payments' table name
@Data // Lombok: Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Lombok: Generates a no-argument constructor
@AllArgsConstructor // Lombok: Generates a constructor with all fields
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for the payment

    private Long orderId; // ID of the order associated with this payment

    private Double amount; // Amount of the payment

    private LocalDateTime paymentDate; // Timestamp of the payment

    private String status; // Current status of the payment (e.g., PENDING, COMPLETED, FAILED, REFUNDED)

    private String paymentMethod; // e.g., CREDIT_CARD, DEBIT_CARD, UPI, WALLET

    // Note: Customer ID and Restaurant ID could be denormalized here or fetched via
    // inter-service communication if needed. For simplicity, we'll keep it focused
    // on the payment itself for now.
}
