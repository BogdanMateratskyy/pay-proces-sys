package com.bmsoftware.payment.service;

import com.bmsoftware.payment.repository.PaymentRepository;
import com.bmsoftware.shared.dto.PaymentProcessedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatusListener {

  private final PaymentRepository paymentRepository;
  private final ObjectMapper objectMapper;

  @KafkaListener(
      topics = "${spring.kafka.payment-processed-topic}",
      groupId = "${spring.kafka.consumer.group-id}")
  @Transactional
  public void handlePaymentProcessed(String payload) {
    log.info("Received payment processed event: {}", payload);
    try {
      PaymentProcessedEvent event = objectMapper.readValue(payload, PaymentProcessedEvent.class);
      paymentRepository
          .findById(event.paymentId())
          .ifPresent(
              payment -> {
                PaymentStatus newStatus = event.status();
                payment.setStatus(newStatus);
                // In a real system, we'd also store the transactionId and errorMessage
                paymentRepository.save(payment);
                log.info("Updated payment ID: {} status to: {}", event.paymentId(), newStatus);
              });
    } catch (Exception e) {
      log.error("Error processing payment processed event", e);
    }
  }
}
