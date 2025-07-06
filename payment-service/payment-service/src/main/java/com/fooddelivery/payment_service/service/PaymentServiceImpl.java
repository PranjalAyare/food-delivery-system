
// // payment-service/src/main/java/com/fooddelivery/payment_service/service/PaymentServiceImpl.java
package com.fooddelivery.payment_service.service;

import com.fooddelivery.payment_service.dto.PaymentRequest;
import com.fooddelivery.payment_service.dto.PaymentResponse;
import com.fooddelivery.payment_service.dto.StripeRequestDto;
import com.fooddelivery.payment_service.model.Payment;
import com.fooddelivery.payment_service.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeSecretKey;
        log.info("✅ Stripe API key initialized.");
    }

    @Override
    @Transactional
    public Payment processPayment(Payment payment) {
        log.info("💳 Processing internal payment for order ID: {}", payment.getOrderId());
        payment.setStatus("PENDING");
        payment.setPaymentDate(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> getPaymentById(Long id) {
        log.info("🔍 Fetching payment by ID: {}", id);
        return paymentRepository.findById(id);
    }

    @Override
    public List<Payment> getAllPayments() {
        log.info("📦 Fetching all payments");
        return paymentRepository.findAll();
    }

    @Override
    @Transactional
    public Optional<Payment> updatePayment(Long id, Payment updatedPayment) {
        log.info("🔄 Attempting to update payment with ID: {}", id);
        return paymentRepository.findById(id).map(existingPayment -> {
            existingPayment.setOrderId(updatedPayment.getOrderId());
            existingPayment.setAmount(updatedPayment.getAmount());
            existingPayment.setPaymentMethod(updatedPayment.getPaymentMethod());

            if (updatedPayment.getStatus() != null && !updatedPayment.getStatus().isEmpty()) {
                existingPayment.setStatus(updatedPayment.getStatus());
            }

            log.info("✅ Payment updated. ID: {}, New Status: {}", id, existingPayment.getStatus());
            return paymentRepository.save(existingPayment);
        });
    }

    @Override
    @Transactional
    public boolean deletePayment(Long id) {
        log.info("🗑️ Attempting to delete payment with ID: {}", id);
        if (paymentRepository.existsById(id)) {
            paymentRepository.deleteById(id);
            log.info("✅ Payment with ID {} deleted.", id);
            return true;
        }
        log.warn("⚠️ Payment with ID {} not found for deletion.", id);
        return false;
    }

    @Override
    @Transactional
    public Optional<Payment> updatePaymentStatus(Long paymentId, String newStatus) {
        log.info("🔧 Updating payment status. ID: {}, New Status: {}", paymentId, newStatus);
        return paymentRepository.findById(paymentId).map(payment -> {
            payment.setStatus(newStatus);
            return paymentRepository.save(payment);
        });
    }

    @Override
    public PaymentResponse processStripePayment(PaymentRequest paymentRequest) {
        log.info("💰 Simulating Stripe payment for order ID: {}", paymentRequest.getOrderId());

        // Simulate Stripe charge logic (replace with actual Stripe charge API if needed)
        return new PaymentResponse(
                paymentRequest.getOrderId(),
                paymentRequest.getAmount(),
                "SUCCESS",
                "dummy-transaction-id",
                "Payment processed successfully"
        );
    }

    @Override
public String createStripeCheckoutSession(StripeRequestDto request) throws Exception {
    log.info("🧾 Creating Stripe checkout session for order ID: {}", request.getOrderId());

    try {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED) // ✅ Ask for billing
                .setCustomerEmail("test@example.com") // ✅ (Optional) Replace with actual user email from frontend
                .setSuccessUrl(request.getSuccessUrl())
                .setCancelUrl(request.getCancelUrl())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(request.getCurrency())
                                                .setUnitAmount(request.getAmount().multiply(new BigDecimal(100)).longValue())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Order #" + request.getOrderId())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        log.info("✅ Stripe session created. URL: {}", session.getUrl());

        return session.getUrl();

    } catch (Exception e) {
        log.error("❌ Failed to create Stripe session: {}", e.getMessage(), e);
        throw new Exception("Stripe checkout session creation failed: " + e.getMessage());
    }
}


}
//abpve
// payment-service/src/main/java/com/fooddelivery/payment_service/service/PaymentServiceImpl.java
// payment-service/src/main/java/com/fooddelivery/payment_service/service/PaymentServiceImpl.java
// package com.fooddelivery.payment_service.service;

// import com.fooddelivery.payment_service.dto.PaymentRequest;
// import com.fooddelivery.payment_service.dto.PaymentResponse;
// import com.fooddelivery.payment_service.dto.StripeRequestDto;
// import com.fooddelivery.payment_service.model.Payment;
// import com.fooddelivery.payment_service.repository.PaymentRepository;
// import com.stripe.Stripe;
// import com.stripe.model.PaymentIntent; // ADDED: Import for PaymentIntent
// import com.stripe.model.checkout.Session;
// import com.stripe.param.PaymentIntentCreateParams; // ADDED: Import for PaymentIntentCreateParams
// import com.stripe.param.checkout.SessionCreateParams;
// import jakarta.annotation.PostConstruct;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.math.BigDecimal;
// import java.util.Date;
// import java.util.List;
// import java.util.Optional;

// @Service
// public class PaymentServiceImpl implements PaymentService {

//     private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

//     private final PaymentRepository paymentRepository;

//     @Value("${stripe.secret.key}")
//     private String stripeSecretKey;

//     public PaymentServiceImpl(PaymentRepository paymentRepository) {
//         this.paymentRepository = paymentRepository;
//     }

//     @PostConstruct
//     public void initStripe() {
//         Stripe.apiKey = stripeSecretKey;
//         log.info("✅ Stripe API key initialized.");
//     }

//     // --- Core CRUD Operations ---

//     @Override
//     @Transactional
//     public Payment createPayment(Payment payment) {
//         // @PrePersist in Payment model handles createdAt and initial status (if not set)
//         log.info("💾 Saving new payment record for order ID: {}", payment.getOrderId());
//         if (payment.getStatus() == null || payment.getStatus().isEmpty()) {
//             payment.setStatus("PENDING"); // Default status if not provided by request
//         }
//         return paymentRepository.save(payment);
//     }

//     @Override
//     public Optional<Payment> getPaymentById(Long id) {
//         log.info("🔍 Fetching payment by ID: {}", id);
//         return paymentRepository.findById(id);
//     }

//     @Override
//     public List<Payment> getAllPayments() {
//         log.info("🌐 Fetching all payment records.");
//         return (List<Payment>) paymentRepository.findAll();
//     }

//     @Override
//     @Transactional
//     public Optional<Payment> updatePayment(Long id, Payment updatedPaymentDetails) {
//         log.info("🔄 Attempting to update payment ID: {}", id);
//         return paymentRepository.findById(id).map(existingPayment -> {
//             // Update fields from updatedPaymentDetails
//             // Only update fields that are typically mutable via an update request
//             if (updatedPaymentDetails.getUserId() != null) {
//                 existingPayment.setUserId(updatedPaymentDetails.getUserId());
//             }
//             if (updatedPaymentDetails.getOrderId() != null && !updatedPaymentDetails.getOrderId().isEmpty()) {
//                 existingPayment.setOrderId(updatedPaymentDetails.getOrderId());
//             }
//             if (updatedPaymentDetails.getAmount() != null) {
//                 existingPayment.setAmount(updatedPaymentDetails.getAmount());
//             }
//             if (updatedPaymentDetails.getStatus() != null && !updatedPaymentDetails.getStatus().isEmpty()) {
//                 existingPayment.setStatus(updatedPaymentDetails.getStatus());
//             }
//             if (updatedPaymentDetails.getPaymentMethod() != null && !updatedPaymentDetails.getPaymentMethod().isEmpty()) {
//                 existingPayment.setPaymentMethod(updatedPaymentDetails.getPaymentMethod());
//             }
//             // THIS IS THE CRUCIAL LINE FOR THE TEST FAILURE: Ensure transactionId is copied
//             if (updatedPaymentDetails.getTransactionId() != null && !updatedPaymentDetails.getTransactionId().isEmpty()) {
//                 existingPayment.setTransactionId(updatedPaymentDetails.getTransactionId());
//             }
//             // createdAt is usually not updated in an update operation.

//             log.info("💾 Saving updated payment ID: {}", existingPayment.getId());
//             return paymentRepository.save(existingPayment);
//         });
//     }

//     @Override
//     @Transactional
//     public Optional<Payment> updatePaymentStatus(Long id, String status) {
//         log.info("🔧 Attempting to update status for payment ID: {} to {}", id, status);
//         return paymentRepository.findById(id).map(payment -> {
//             payment.setStatus(status);
//             log.info("💾 Saving payment ID: {} with new status: {}", payment.getId(), status);
//             return paymentRepository.save(payment);
//         });
//     }

//     @Override
//     @Transactional
//     public boolean deletePayment(Long id) {
//         log.info("🗑️ Attempting to delete payment ID: {}", id);
//         if (paymentRepository.existsById(id)) {
//             paymentRepository.deleteById(id);
//             log.info("✅ Payment ID: {} deleted.", id);
//             return true;
//         }
//         log.warn("⚠️ Payment ID: {} not found for deletion.", id);
//         return false;
//     }

//     // --- User-specific Retrieval Methods ---
//     @Override
//     public List<Payment> getPaymentsByUserId(Long userId) {
//         log.info("🔍 Fetching payments for user ID: {}", userId);
//         return paymentRepository.findByUserId(userId);
//     }

//     @Override
//     public Optional<Payment> getPaymentByIdAndUserId(Long paymentId, Long userId) {
//         log.info("🔍 Fetching payment by ID: {} for user ID: {}", paymentId, userId);
//         return paymentRepository.findByIdAndUserId(paymentId, userId);
//     }

//     // --- Stripe Integration Methods ---
//     @Override
//     @Transactional
//     public PaymentResponse processStripePayment(PaymentRequest request) {
//         log.info("⚡ Processing Stripe payment for orderId: {} amount: {} {}", request.getOrderId(), request.getAmount(), request.getCurrency());
//         try {
//             // Convert BigDecimal amount to long cents/paise for Stripe
//             long amountCents = request.getAmount().multiply(new BigDecimal("100")).longValueExact();

//             PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
//                     .setAmount(amountCents)
//                     .setCurrency(request.getCurrency())
//                     .addPaymentMethodType("card") // Assuming card payments for this direct charge
//                     .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL) // Or AUTOMATIC
//                     .setConfirm(true) // Confirm immediately
//                     .setPaymentMethod(request.getStripeToken()) // Use the token from the client
//                     .build();

//             PaymentIntent paymentIntent = PaymentIntent.create(createParams); // Actual Stripe call

//             String transactionId = paymentIntent.getId();
//             String status = "FAILED"; // Default to FAILED
//             String message = "Payment processing failed";

//             if ("succeeded".equals(paymentIntent.getStatus())) {
//                 status = "COMPLETED";
//                 message = "Payment successful";
//             } else if ("requires_action".equals(paymentIntent.getStatus())) {
//                 status = "PENDING"; // Payment needs 3D Secure or other action
//                 message = "Payment requires further action (e.g., 3D Secure)";
//             } else if ("requires_confirmation".equals(paymentIntent.getStatus())) {
//                 status = "PENDING";
//                 message = "Payment requires confirmation";
//             } else if ("requires_payment_method".equals(paymentIntent.getStatus())) {
//                 message = "Payment method declined or invalid";
//             } else if ("canceled".equals(paymentIntent.getStatus())) {
//                 message = "Payment was canceled";
//             }


//             Payment payment = new Payment();
//             payment.setOrderId(request.getOrderId());
//             payment.setAmount(request.getAmount());
//             payment.setPaymentMethod("STRIPE_CARD");
//             payment.setStatus(status);
//             payment.setTransactionId(transactionId);
//             // userId for this payment will be handled by the controller via getUserIdFromRequest()
//             // and then set on the Payment object before createInternalPayment is called.
//             // If processStripePayment is ever called directly by another service without an associated
//             // userId in a DTO, you'd need to consider how userId is obtained/assigned.

//             Payment savedPayment = paymentRepository.save(payment); // Save the payment record

//             log.info("Stripe Payment Intent Status: {}, Transaction ID: {}", paymentIntent.getStatus(), transactionId);
//             return new PaymentResponse(
//                     savedPayment.getOrderId(),
//                     savedPayment.getAmount(),
//                     savedPayment.getStatus(),
//                     savedPayment.getTransactionId(),
//                     message
//             );

//         } catch (com.stripe.exception.StripeException e) {
//             log.error("Stripe API error during payment processing: {}", e.getMessage(), e);
//             throw new RuntimeException("Stripe API error: " + e.getMessage(), e);
//         } catch (Exception e) {
//             log.error("General error during payment processing: {}", e.getMessage(), e);
//             throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
//         }
//     }

//     @Override
//     public String createStripeCheckoutSession(StripeRequestDto request) {
//         log.info("🛒 Creating Stripe Checkout Session for orderId: {} amount: {} {}", request.getOrderId(), request.getAmount(), request.getCurrency());
//         try {
//             long amountCents = request.getAmount().multiply(new BigDecimal("100")).longValueExact();

//             SessionCreateParams params = SessionCreateParams.builder()
//                     .setMode(SessionCreateParams.Mode.PAYMENT)
//                     .setSuccessUrl(request.getSuccessUrl())
//                     .setCancelUrl(request.getCancelUrl())
//                     .addLineItem(
//                             SessionCreateParams.LineItem.builder()
//                                     .setQuantity(1L)
//                                     .setPriceData(
//                                             SessionCreateParams.LineItem.PriceData.builder()
//                                                     .setCurrency(request.getCurrency())
//                                                     .setUnitAmount(amountCents)
//                                                     .setProductData(
//                                                             SessionCreateParams.LineItem.PriceData.ProductData.builder()
//                                                                     .setName("Order: " + request.getOrderId())
//                                                                     .build())
//                                                     .build())
//                                     .build())
//                     .build();

//             Session session = Session.create(params); // Actual Stripe call
//             log.info("Stripe Checkout Session created with ID: {}", session.getId());
//             return session.getUrl();

//         } catch (com.stripe.exception.StripeException e) {
//             log.error("Stripe API error during checkout session creation: {}", e.getMessage(), e);
//             throw new RuntimeException("Stripe checkout session creation failed: " + e.getMessage(), e);
//         } catch (Exception e) {
//             log.error("General error during checkout session creation: {}", e.getMessage(), e);
//             throw new RuntimeException("Checkout session creation failed: " + e.getMessage(), e);
//         }
//     }
// }