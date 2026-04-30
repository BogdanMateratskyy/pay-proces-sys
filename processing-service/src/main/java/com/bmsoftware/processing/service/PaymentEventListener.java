package com.bmsoftware.processing.service;

import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.bmsoftware.shared.dto.PaymentProcessedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventListener {

  private final ObjectMapper objectMapper;
  private final RoutingService routingService;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @KafkaListener(
      topics = "${spring.kafka.consumer.payment-created-topic}",
      groupId = "${spring.kafka.consumer.group-id}")
  public void handlePaymentCreated(String payload) {
    log.info("Received payment created event: {}", payload);
    try {
      PaymentCreatedEvent event = objectMapper.readValue(payload, PaymentCreatedEvent.class);
      log.info("Successfully parsed event for payment ID: {}", event.paymentId());

      processPayment(event);
    } catch (Exception e) {
      log.error("Error parsing payment created event", e);
    }
  }

  private void processPayment(PaymentCreatedEvent event) {
    try {
      var response = routingService.routePayment(event);
      log.info(
          "Payment processed by bank. Status: {}, Transaction ID: {}",
          response.status(),
          response.transactionId());

      publishProcessedEvent(event, response.status(), response.transactionId(), null);
    } catch (Exception e) {
      log.error("Failed to route payment ID: {}", event.paymentId(), e);
      publishProcessedEvent(event, PaymentStatus.FAILED, null, e.getMessage());
    }
  }

  private void publishProcessedEvent(
      PaymentCreatedEvent event, PaymentStatus status, String transactionId, String error) {
    try {
      PaymentProcessedEvent processedEvent =
          new PaymentProcessedEvent(event.paymentId(), status, transactionId, error);
      String payload = objectMapper.writeValueAsString(processedEvent);
      kafkaTemplate.send("payments.processed", event.paymentId().toString(), payload);
      log.info("Published payment processed event for ID: {}", event.paymentId());
    } catch (Exception e) {
      log.error("Failed to publish processed event for ID: {}", event.paymentId(), e);
    }
  }
}
