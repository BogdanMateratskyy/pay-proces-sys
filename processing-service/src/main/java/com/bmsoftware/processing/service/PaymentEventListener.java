package com.bmsoftware.processing.service;

import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventListener {

  private final ObjectMapper objectMapper;

  @KafkaListener(topics = "payments.created", groupId = "processing-group")
  public void handlePaymentCreated(String payload) {
    log.info("Received payment created event: {}", payload);
    try {
      PaymentCreatedEvent event = objectMapper.readValue(payload, PaymentCreatedEvent.class);
      log.info("Successfully parsed event for payment ID: {}", event.paymentId());

      // Implement Routing Logic here
    } catch (Exception e) {
      log.error("Error parsing payment created event", e);
    }
  }
}
