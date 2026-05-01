package com.bmsoftware.bank.controller;

import com.bmsoftware.shared.dto.BankPaymentRequest;
import com.bmsoftware.shared.dto.BankPaymentResponse;
import com.bmsoftware.shared.dto.PaymentStatus;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/banks")
public class BankAdapterController {

  @PostMapping("/bank-a/process")
  public BankPaymentResponse processBankA(@RequestBody BankPaymentRequest request) {
    log.info("Bank A processing payment: {}", request.paymentId());
    return new BankPaymentResponse(
        request.paymentId(), PaymentStatus.SUCCESS, UUID.randomUUID().toString(), null);
  }

  @PostMapping("/bank-b/process")
  public BankPaymentResponse processBankB(@RequestBody BankPaymentRequest request) {
    log.info("Bank B processing payment: {}", request.paymentId());
    return new BankPaymentResponse(
        request.paymentId(), PaymentStatus.SUCCESS, UUID.randomUUID().toString(), null);
  }
}
