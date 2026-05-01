package com.bmsoftware.shared.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BankPaymentRequest(
    UUID paymentId,
    BigDecimal amount,
    String currency,
    String recipientAccount,
    String senderAccount) {}
