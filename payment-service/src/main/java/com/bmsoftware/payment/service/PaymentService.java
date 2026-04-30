package com.bmsoftware.payment.service;

import com.bmsoftware.payment.dto.PaymentRequest;
import com.bmsoftware.payment.dto.PaymentResponse;
import com.bmsoftware.payment.mapper.PaymentMapper;
import com.bmsoftware.payment.model.OutboxEvent;
import com.bmsoftware.payment.model.Payment;
import com.bmsoftware.payment.repository.OutboxRepository;
import com.bmsoftware.payment.repository.PaymentRepository;
import com.bmsoftware.shared.dto.AggregateType;
import com.bmsoftware.shared.dto.EventType;
import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final OutboxRepository outboxRepository;
  private final PaymentMapper paymentMapper;
  private final ObjectMapper objectMapper;

  @Transactional
  public PaymentResponse initiatePayment(PaymentRequest request) {
    log.info("Initiating payment for amount: {} {}", request.amount(), request.currency());

    Payment payment = paymentMapper.toEntity(request, PaymentStatus.PENDING);
    Payment savedPayment = paymentRepository.save(payment);

    log.info(
        "Payment saved with ID: {} and status: {}", savedPayment.getId(), savedPayment.getStatus());

    saveOutboxEvent(savedPayment);

    return paymentMapper.toResponse(savedPayment, "Payment initiated successfully");
  }

  private void saveOutboxEvent(Payment payment) {
    try {
      PaymentCreatedEvent event = paymentMapper.toPaymentCreatedEvent(payment);

      String payload = objectMapper.writeValueAsString(event);

      OutboxEvent outboxEvent =
          OutboxEvent.builder()
              .aggregateId(payment.getId())
              .aggregateType(AggregateType.PAYMENT)
              .eventType(EventType.PAYMENT_CREATED)
              .payload(payload)
              .processed(false)
              .build();

      outboxRepository.save(outboxEvent);
      log.info("Outbox event saved for payment ID: {}", payment.getId());
    } catch (JsonProcessingException e) {
      log.error("Error serializing payment event for outbox", e);
      throw new RuntimeException("Failed to initiate payment due to serialization error", e);
    }
  }
}
