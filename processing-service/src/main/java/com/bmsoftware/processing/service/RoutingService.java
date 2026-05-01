package com.bmsoftware.processing.service;

import com.bmsoftware.processing.client.BankAdapterClient;
import com.bmsoftware.shared.dto.BankPaymentRequest;
import com.bmsoftware.shared.dto.BankPaymentResponse;
import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

  private final BankAdapterClient bankAdapterClient;

  public BankPaymentResponse routePayment(PaymentCreatedEvent event) {
    log.info("Routing payment ID: {} with amount: {}", event.paymentId(), event.amount());

    BankPaymentRequest request =
        new BankPaymentRequest(
            event.paymentId(),
            event.amount(),
            event.currency(),
            event.recipientAccount(),
            event.senderAccount());

    // Simple routing logic: Amount > 1000 goes to Bank B, otherwise Bank A
    if (event.amount().compareTo(new BigDecimal("1000")) > 0) {
      log.info("Routing to Bank B");
      return bankAdapterClient.processBankB(request);
    } else {
      log.info("Routing to Bank A");
      return bankAdapterClient.processBankA(request);
    }
  }
}
