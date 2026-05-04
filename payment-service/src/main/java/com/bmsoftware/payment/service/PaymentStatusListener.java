package com.bmsoftware.payment.service;

import com.bmsoftware.shared.dto.PaymentProcessedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatusListener {

  private final PaymentService paymentService;
  private final ObjectMapper objectMapper;

  @KafkaListener(
      topics = "${spring.kafka.payment-processed-topic}",
      groupId = "${spring.kafka.consumer.group-id}")
  public void handlePaymentProcessed(String payload) {
    log.info("Received payment processed event: {}", payload);
    try {
      PaymentProcessedEvent event = objectMapper.readValue(payload, PaymentProcessedEvent.class);
      paymentService.updateStatus(event);
    } catch (Exception e) {
      log.error("Error processing payment processed event", e);
    }
  }
}
