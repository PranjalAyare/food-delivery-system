package com.fooddelivery.order_service.dto;



import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;



// This DTO mirrors the expected request body for PaymentService's POST /payments endpoint.

// It will be used by the OrderService to send payment initiation requests.

@Data // Lombok: Generates getters, setters, toString, equals, and hashCode

@NoArgsConstructor // Lombok: Generates a no-argument constructor

@AllArgsConstructor // Lombok: Generates a constructor with all fields

public class PaymentRequest {
private Long orderId;
private Double amount;
private String paymentMethod;

}
