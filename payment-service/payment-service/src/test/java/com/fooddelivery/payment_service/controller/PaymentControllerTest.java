
package com.fooddelivery.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.payment_service.config.TestSecurityConfig;
import com.fooddelivery.payment_service.dto.PaymentRequest;
import com.fooddelivery.payment_service.dto.PaymentResponse;
import com.fooddelivery.payment_service.dto.StripeRequestDto;
import com.fooddelivery.payment_service.model.Payment;
import com.fooddelivery.payment_service.security.JwtFilter;
import com.fooddelivery.payment_service.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PaymentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class)
)
@Import(TestSecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment(1L, 101L, 50.0, LocalDateTime.now(), "COMPLETED", "CREDIT_CARD");
        objectMapper.findAndRegisterModules();
    }

    // ✅ /payments/create-checkout-session – success
    @Test
    @WithMockUser
    void testCreateCheckoutSession_Success() throws Exception {
        StripeRequestDto request = new StripeRequestDto();
        request.setOrderId("ORD-200");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");
        request.setSuccessUrl("http://success.com");
        request.setCancelUrl("http://cancel.com");

        when(paymentService.createStripeCheckoutSession(any(StripeRequestDto.class)))
                .thenReturn("http://mock.stripe/checkout");

        mockMvc.perform(post("/payments/create-checkout-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://mock.stripe/checkout"));

        verify(paymentService).createStripeCheckoutSession(any(StripeRequestDto.class));
    }

    // ❌ /payments/create-checkout-session – failure
    @Test
    @WithMockUser
    void testCreateCheckoutSession_Failure() throws Exception {
        StripeRequestDto request = new StripeRequestDto();
        request.setOrderId("ORD-200");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");
        request.setSuccessUrl("http://success.com");
        request.setCancelUrl("http://cancel.com");

        when(paymentService.createStripeCheckoutSession(any(StripeRequestDto.class)))
                .thenThrow(new RuntimeException("Stripe service failed"));

        mockMvc.perform(post("/payments/create-checkout-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to create Stripe Checkout session"));

        verify(paymentService).createStripeCheckoutSession(any(StripeRequestDto.class));
    }

    // ⚠️ POST /payments – invalid input
    @Test
    @WithMockUser
    void testProcessPayment_InvalidInput() throws Exception {
        PaymentRequest invalidRequest = new PaymentRequest(null, null, null, null); // Missing required fields

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest()); // Should fail @Valid

        verifyNoInteractions(paymentService);
    }

    
    // testProcessPayment_Authenticated
    // testProcessPayment_Unauthenticated
    // testGetAllPayments_Authenticated
    // testGetPaymentById_Found
    // testGetPaymentById_NotFound
    // testUpdatePayment_Found
    // testUpdatePayment_NotFound
    // testUpdatePaymentStatus_Found
    // testUpdatePaymentStatus_NotFound
    // testDeletePayment_Found
    // testDeletePayment_NotFound
}
