package com.bmsoftware.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PaymentRequest(
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive")
        BigDecimal amount,
    @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,
    @NotBlank(message = "Recipient account is required") String recipientAccount,
    @NotBlank(message = "Sender account is required") String senderAccount) {}
