
// payment-service/src/main/java/com/fooddelivery/payment_service/controller/PaymentController.java
package com.fooddelivery.payment_service.controller;

import com.fooddelivery.payment_service.dto.PaymentRequest;
import com.fooddelivery.payment_service.dto.PaymentResponse;
import com.fooddelivery.payment_service.dto.StripeRequestDto;
import com.fooddelivery.payment_service.model.Payment;
import com.fooddelivery.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /payments – Stripe payment (legacy direct processing)
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        log.info("🔁 Processing Stripe payment for Order ID: {}", paymentRequest.getOrderId());
        try {
            PaymentResponse response = paymentService.processStripePayment(paymentRequest);
            log.info("✅ Stripe payment successful: {}", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("❌ Payment failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PaymentResponse(
                            paymentRequest.getOrderId(),
                            paymentRequest.getAmount(),
                            "FAILED",
                            null,
                            "Payment failed: " + e.getMessage()
                    ));
        }
    }

    /**
     * ✅ POST /payments/create-checkout-session – Stripe hosted payment session
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody StripeRequestDto request) {
        log.info("🧾 Creating Stripe Checkout Session for Order ID: {}", request.getOrderId());
        try {
            String checkoutUrl = paymentService.createStripeCheckoutSession(request);
            Map<String, String> response = new HashMap<>();
            response.put("url", checkoutUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Stripe session creation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create Stripe Checkout session"));
        }
    }

    /**
     * GET /payments – List all payments
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        log.info("📦 Fetching all payment records.");
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * GET /payments/{id} – Get payment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable Long id) {
        log.info("🔍 Fetching payment ID: {}", id);
        return paymentService.getPaymentById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found"));
    }

    /**
     * PUT /payments/{id} – Update full payment
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePayment(@PathVariable Long id, @Valid @RequestBody Payment payment) {
        log.info("🔁 Updating payment ID: {}", id);
        return paymentService.updatePayment(id, payment)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found"));
    }

    /**
     * PATCH /payments/{id}/status – Update payment status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable Long id, @RequestBody String status) {
        log.info("🔧 Updating status for payment ID {}: {}", id, status);
        return paymentService.updatePaymentStatus(id, status)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found"));
    }

    /**
     * DELETE /payments/{id} – Delete payment
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        log.info("🗑️ Deleting payment ID: {}", id);
        if (paymentService.deletePayment(id)) {
            log.info("✅ Payment ID {} deleted successfully", id);
            return ResponseEntity.noContent().build();
        } else {
            log.warn("⚠️ Payment ID {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }
}
