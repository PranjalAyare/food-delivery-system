
// payment-service/src/main/java/com/fooddelivery/payment_service/service/PaymentService.java
package com.fooddelivery.payment_service.service;

import com.fooddelivery.payment_service.dto.PaymentRequest;
import com.fooddelivery.payment_service.dto.PaymentResponse;
import com.fooddelivery.payment_service.dto.StripeRequestDto;
import com.fooddelivery.payment_service.model.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentService {

    /**
     * Processes a new internal payment.
     * Sets initial status (e.g., PENDING) and payment date.
     *
     * @param payment The Payment object containing details for the new payment.
     * @return The saved Payment object with updated status and date.
     */
    Payment processPayment(Payment payment);

    /**
     * Retrieves a payment by its unique ID.
     *
     * @param id The ID of the payment to retrieve.
     * @return An Optional containing the Payment if found, or empty if not.
     */
    Optional<Payment> getPaymentById(Long id);

    /**
     * Retrieves all payments.
     *
     * @return A list of all Payment objects.
     */
    List<Payment> getAllPayments();

    /**
     * Updates an existing payment's details.
     *
     * @param id The ID of the payment to update.
     * @param updatedPayment The Payment object containing the updated details.
     * @return An Optional containing the updated Payment if found and updated, or empty if not found.
     */
    Optional<Payment> updatePayment(Long id, Payment updatedPayment);

    /**
     * Deletes a payment by its unique ID.
     *
     * @param id The ID of the payment to delete.
     * @return true if the payment was successfully deleted, false otherwise.
     */
    boolean deletePayment(Long id);

    /**
     * Updates the status of a specific payment.
     *
     * @param paymentId The ID of the payment whose status is to be updated.
     * @param newStatus The new status to set (e.g., "COMPLETED", "FAILED", "REFUNDED").
     * @return An Optional containing the updated Payment if found, or empty if not.
     */
    Optional<Payment> updatePaymentStatus(Long paymentId, String newStatus);

    /**
     * Processes a payment using direct Stripe charge API (legacy).
     *
     * @param paymentRequest The details of the Stripe payment request.
     * @return A response DTO with payment status and transaction ID.
     */
    PaymentResponse processStripePayment(PaymentRequest paymentRequest);

    /**
     * Creates a Stripe Checkout session and returns the session URL.
     *
     * @param request Details like amount, currency, and order ID.
     * @return The URL to redirect the customer to for Stripe-hosted payment.
     * @throws Exception if session creation fails.
     */
    String createStripeCheckoutSession(StripeRequestDto request) throws Exception;
}
