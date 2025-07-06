package com.fooddelivery.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String orderId;
    private BigDecimal amount;
    private String status;         // "SUCCESS" or "FAILED"
    private String transactionId;  // Stripe charge ID or internal ID
    private String message;        // Human-readable message
}
