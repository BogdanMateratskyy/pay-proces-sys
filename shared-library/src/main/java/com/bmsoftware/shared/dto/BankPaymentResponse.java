package com.bmsoftware.shared.dto;

import java.util.UUID;

public record BankPaymentResponse(
    UUID paymentId, PaymentStatus status, String transactionId, String errorMessage) {}
