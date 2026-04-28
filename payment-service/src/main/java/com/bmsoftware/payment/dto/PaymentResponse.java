package com.bmsoftware.payment.dto;

import java.util.UUID;

public record PaymentResponse(UUID paymentId, String status, String message) {}
