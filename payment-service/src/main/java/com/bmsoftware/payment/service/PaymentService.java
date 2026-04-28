package com.bmsoftware.payment.service;

import com.bmsoftware.payment.dto.PaymentRequest;
import com.bmsoftware.payment.dto.PaymentResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentService {

  public PaymentResponse initiatePayment(PaymentRequest request) {
    log.info("Initiating payment for amount: {} {}", request.amount(), request.currency());

    return new PaymentResponse(UUID.randomUUID(), "PENDING", "Payment initiated successfully");
  }
}
