package com.bmsoftware.payment.service;

import com.bmsoftware.payment.dto.PaymentRequest;
import com.bmsoftware.payment.dto.PaymentResponse;
import com.bmsoftware.payment.mapper.PaymentMapper;
import com.bmsoftware.payment.model.OutboxEvent;
import com.bmsoftware.payment.model.Payment;
import com.bmsoftware.payment.model.PaymentStatusEntity;
import com.bmsoftware.payment.repository.PaymentRepository;
import com.bmsoftware.shared.dto.AggregateType;
import com.bmsoftware.shared.dto.EventType;
import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.bmsoftware.shared.dto.PaymentProcessedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final OutboxService outboxService;
  private final PaymentAuditLogService auditLogService;
  private final PaymentMapper paymentMapper;
  private final ObjectMapper objectMapper;

  public Optional<Payment> findById(UUID id) {
    return paymentRepository.findById(id);
  }

  @Transactional
  public void updateStatus(PaymentProcessedEvent event) {
    UUID paymentId = event.paymentId();
    findById(paymentId)
        .ifPresentOrElse(
            payment -> updatePaymentStatus(event, payment),
            () -> log.warn("Payment ID: {} not found, skipping status update", paymentId));
  }

  private void updatePaymentStatus(PaymentProcessedEvent event, Payment payment) {
    PaymentStatus newStatus = event.status();
    PaymentStatus previousStatus =
        payment.getStatus() != null ? payment.getStatus().getStatus() : null;
    PaymentStatusEntity statusEntity =
        PaymentStatusEntity.builder()
            .payment(payment)
            .status(newStatus)
            .transactionId(event.transactionId())
            .errorMessage(event.errorMessage())
            .build();
    payment.setStatus(statusEntity);
    paymentRepository.save(payment);
    auditLogService.saveAuditLog(
        payment, previousStatus, newStatus, event.transactionId(), event.errorMessage());
    log.info(
        "Payment ID: {} status transition: {} -> {}", payment.getId(), previousStatus, newStatus);
  }

  @Transactional
  public PaymentResponse initiatePayment(PaymentRequest request) {
    log.info("Initiating payment for amount: {} {}", request.amount(), request.currency());
    Payment payment = paymentMapper.toEntity(request);
    PaymentStatusEntity initialStatus =
        PaymentStatusEntity.builder().payment(payment).status(PaymentStatus.PENDING).build();
    payment.setStatus(initialStatus);
    Payment savedPayment = paymentRepository.save(payment);
    auditLogService.saveAuditLog(savedPayment, null, PaymentStatus.PENDING, null, null);
    log.info("Payment saved with ID: {} and status: PENDING", savedPayment.getId());
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
      outboxService.save(outboxEvent);
      log.info("Outbox event saved for payment ID: {}", payment.getId());
    } catch (JsonProcessingException e) {
      log.error("Error serializing payment event for outbox", e);
      throw new RuntimeException("Failed to initiate payment due to serialization error", e);
    }
  }
}
