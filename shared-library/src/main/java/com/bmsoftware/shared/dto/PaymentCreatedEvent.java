package com.bmsoftware.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCreatedEvent(
    UUID paymentId,
    BigDecimal amount,
    String currency,
    String recipientAccount,
    String senderAccount,
    LocalDateTime createdAt,
    PaymentStatus status) {}
