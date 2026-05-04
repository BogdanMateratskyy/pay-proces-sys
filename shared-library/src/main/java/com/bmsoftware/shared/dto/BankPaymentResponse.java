package com.bmsoftware.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BankPaymentResponse(
    UUID paymentId, PaymentStatus status, String transactionId, String errorMessage) {}
