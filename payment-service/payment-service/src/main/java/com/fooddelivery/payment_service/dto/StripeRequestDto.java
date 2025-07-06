package com.fooddelivery.payment_service.dto;

import java.math.BigDecimal;

public class StripeRequestDto {
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String successUrl;
    private String cancelUrl;

    public StripeRequestDto() {}

    public StripeRequestDto(String orderId, BigDecimal amount, String currency, String successUrl, String cancelUrl) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }

    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
}
