package com.fooddelivery.order_service.feign;

import com.fooddelivery.order_service.dto.PaymentRequest; // Import the DTO we just created
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// @FeignClient annotation defines the client.
// "payment-service" must match the spring.application.name of your payment-service in Eureka.
@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    /**
     * This method defines how to call the POST /payments endpoint of the payment-service.
     * Feign will automatically translate this method call into an HTTP POST request
     * to the 'payment-service' instance discovered via Eureka.
     *
     * The RequestInterceptor (which we will create in the next step) will
     * automatically add the Authorization header (JWT token) to this outgoing request.
     *
     * @param paymentRequest The DTO containing the orderId, amount, and paymentMethod.
     * @return A ResponseEntity indicating the result of the payment processing (e.g., 201 Created).
     * The body can be String or a more specific DTO if needed to parse the response.
     */
    @PostMapping("/payments")
    ResponseEntity<String> processPayment(@RequestBody PaymentRequest paymentRequest);

    // You can add other methods here to call other payment-service endpoints as needed, e.g.:
    // @GetMapping("/payments/{id}")
    // ResponseEntity<PaymentResponse> getPaymentById(@PathVariable("id") Long paymentId);
}
