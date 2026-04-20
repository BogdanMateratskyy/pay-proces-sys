package com.bmsoftware.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreatedEvent {
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
    private String recipientAccount;
    private String senderAccount;
    private LocalDateTime createdAt;
    private String status;
}
