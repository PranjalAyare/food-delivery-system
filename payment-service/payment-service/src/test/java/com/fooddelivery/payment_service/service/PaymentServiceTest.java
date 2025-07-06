package com.fooddelivery.payment_service.service;

import com.fooddelivery.payment_service.model.Payment;
import com.fooddelivery.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock // Mocks the repository dependency
    private PaymentRepository paymentRepository;

    @InjectMocks // Injects the mocked repository into the service implementation
    private PaymentServiceImpl paymentService;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        // Initialize a common test payment object for reusability
        testPayment = new Payment(1L, 101L, 50.0, LocalDateTime.of(2023, 1, 15, 10, 30), "PENDING", "CREDIT_CARD");
    }

    @Test
    void testProcessPayment_setsInitialStatusAndDate() {
        // Given a payment with no ID, status, or date (as received from controller)
        Payment newPayment = new Payment(null, 102L, 75.0, null, null, "DEBIT_CARD");

        // Use ArgumentCaptor to inspect the object passed to the repository's save method
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        // Mock the repository save method to return a simulated saved payment
        // The .thenAnswer ensures we return a new object with an ID, leaving the captured
        // object without an ID (simulating DB ID generation)
        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(invocation -> {
            Payment captured = invocation.getArgument(0);
            // Create a copy to simulate the database assigning an ID and returning the saved entity
            return new Payment(2L, captured.getOrderId(), captured.getAmount(), captured.getPaymentDate(), captured.getStatus(), captured.getPaymentMethod());
        });

        // When processing the payment
        Payment processedPayment = paymentService.processPayment(newPayment);

        // Then verify the returned payment
        assertNotNull(processedPayment);
        assertEquals(2L, processedPayment.getId()); // Should have an ID from mock
        assertEquals("PENDING", processedPayment.getStatus()); // Status should be set by service
        assertNotNull(processedPayment.getPaymentDate()); // Date should be set by service
        assertEquals(102L, processedPayment.getOrderId());

        // Verify the object that was actually passed to the repository
        Payment capturedPayment = paymentCaptor.getValue();
        assertNull(capturedPayment.getId()); // ID should be null before DB saves it
        assertEquals("PENDING", capturedPayment.getStatus());
        assertNotNull(capturedPayment.getPaymentDate());
    }

    @Test
    void testGetPaymentById_found() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        Optional<Payment> result = paymentService.getPaymentById(1L);

        assertTrue(result.isPresent());
        assertEquals(testPayment.getId(), result.get().getId());
        verify(paymentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetPaymentById_notFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Payment> result = paymentService.getPaymentById(99L);

        assertFalse(result.isPresent());
        verify(paymentRepository, times(1)).findById(99L);
    }

    @Test
    void testGetAllPayments() {
        List<Payment> payments = Arrays.asList(
            testPayment,
            new Payment(2L, 103L, 120.0, LocalDateTime.now(), "COMPLETED", "UPI")
        );
        when(paymentRepository.findAll()).thenReturn(payments);

        List<Payment> result = paymentService.getAllPayments();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testPayment.getId(), result.get(0).getId());
        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    void testUpdatePayment_found_updatesAllFields() {
        Payment existingPayment = new Payment(1L, 101L, 50.0, LocalDateTime.of(2023, 1, 15, 10, 30), "PENDING", "CREDIT_CARD");
        Payment updatedDetails = new Payment(null, 200L, 75.5, null, "REFUNDED", "PAYPAL");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(existingPayment));
        // Simulate save returning the modified existing object
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Payment> result = paymentService.updatePayment(1L, updatedDetails);

        assertTrue(result.isPresent());
        Payment actualUpdatedPayment = result.get();
        assertEquals(1L, actualUpdatedPayment.getId()); // ID should remain the same
        assertEquals(200L, actualUpdatedPayment.getOrderId()); // Order ID updated
        assertEquals(75.5, actualUpdatedPayment.getAmount()); // Amount updated
        assertEquals("REFUNDED", actualUpdatedPayment.getStatus()); // Status updated
        assertEquals("PAYPAL", actualUpdatedPayment.getPaymentMethod()); // Payment method updated
        // Ensure paymentDate was not explicitly overwritten if null in updatedDetails
        assertEquals(LocalDateTime.of(2023, 1, 15, 10, 30), actualUpdatedPayment.getPaymentDate());

        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, times(1)).save(existingPayment); // Verify save was called with the modified object
    }

    @Test
    void testUpdatePayment_notFound() {
        Payment updatedDetails = new Payment(null, 200L, 75.5, null, "REFUNDED", "PAYPAL");
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Payment> result = paymentService.updatePayment(99L, updatedDetails);

        assertFalse(result.isPresent());
        verify(paymentRepository, times(1)).findById(99L);
        verify(paymentRepository, never()).save(any(Payment.class)); // Save should not be called
    }

    @Test
    void testUpdatePaymentStatus_found() {
        Payment existingPayment = new Payment(1L, 101L, 50.0, LocalDateTime.now(), "PENDING", "CREDIT_CARD");
        String newStatus = "COMPLETED";

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Payment> result = paymentService.updatePaymentStatus(1L, newStatus);

        assertTrue(result.isPresent());
        assertEquals(newStatus, result.get().getStatus());
        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, times(1)).save(existingPayment); // Verify save was called with the modified object
    }

    @Test
    void testUpdatePaymentStatus_notFound() {
        String newStatus = "COMPLETED";
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Payment> result = paymentService.updatePaymentStatus(99L, newStatus);

        assertFalse(result.isPresent());
        verify(paymentRepository, times(1)).findById(99L);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void testDeletePayment_found() {
        when(paymentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(paymentRepository).deleteById(1L); // Mock a void method

        boolean result = paymentService.deletePayment(1L);

        assertTrue(result);
        verify(paymentRepository, times(1)).existsById(1L);
        verify(paymentRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeletePayment_notFound() {
        when(paymentRepository.existsById(99L)).thenReturn(false);

        boolean result = paymentService.deletePayment(99L);

        assertFalse(result);
        verify(paymentRepository, times(1)).existsById(99L);
        verify(paymentRepository, never()).deleteById(anyLong()); // Should not attempt to delete
    }
}
