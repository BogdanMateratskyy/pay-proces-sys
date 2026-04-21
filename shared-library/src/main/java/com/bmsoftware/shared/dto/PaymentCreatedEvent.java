package com.bmsoftware.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCreatedEvent(
    UUID paymentId,
    BigDecimal amount,
    String currency,
    String recipientAccount,
    String senderAccount,
    LocalDateTime createdAt,
    String status) {}
