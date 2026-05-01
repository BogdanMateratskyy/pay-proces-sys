package com.bmsoftware.shared.dto;

import java.util.UUID;

public record PaymentProcessedEvent(
    UUID paymentId, PaymentStatus status, String transactionId, String errorMessage) {}
