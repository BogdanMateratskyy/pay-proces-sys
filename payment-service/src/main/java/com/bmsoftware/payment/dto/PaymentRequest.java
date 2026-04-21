package com.bmsoftware.payment.dto;

import java.math.BigDecimal;

public record PaymentRequest(
    BigDecimal amount, String currency, String recipientAccount, String senderAccount) {}
