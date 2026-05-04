package com.bmsoftware.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bmsoftware.payment.dto.PaymentRequest;
import com.bmsoftware.payment.dto.PaymentResponse;
import com.bmsoftware.payment.mapper.PaymentMapper;
import com.bmsoftware.payment.model.OutboxEvent;
import com.bmsoftware.payment.model.Payment;
import com.bmsoftware.payment.model.PaymentStatusEntity;
import com.bmsoftware.payment.repository.OutboxRepository;
import com.bmsoftware.payment.repository.PaymentRepository;
import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.bmsoftware.shared.dto.PaymentProcessedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private OutboxRepository outboxRepository;
  @Mock private PaymentMapper paymentMapper;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private PaymentService paymentService;

  private UUID paymentId;
  private Payment payment;

  @BeforeEach
  void setUp() {
    paymentId = UUID.randomUUID();
    payment = new Payment();
    payment.setId(paymentId);
  }

  @Test
  void findById_whenExists_returnsPayment() {
    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    Optional<Payment> result = paymentService.findById(paymentId);

    assertThat(result).isPresent().contains(payment);
  }

  @Test
  void findById_whenNotExists_returnsEmpty() {
    when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

    Optional<Payment> result = paymentService.findById(paymentId);

    assertThat(result).isEmpty();
  }

  @Test
  void updateStatus_whenPaymentFound_updatesStatusAndSaves() {
    PaymentStatusEntity existingStatus =
        PaymentStatusEntity.builder().status(PaymentStatus.PENDING).build();
    payment.setStatus(existingStatus);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    PaymentProcessedEvent event =
        new PaymentProcessedEvent(paymentId, PaymentStatus.SUCCESS, "TXN-001", null);
    paymentService.updateStatus(event);

    ArgumentCaptor<Payment> capturedPayment = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(capturedPayment.capture());
    Payment saved = capturedPayment.getValue();
    assertThat(saved.getStatus().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(saved.getStatus().getTransactionId()).isEqualTo("TXN-001");
    assertThat(saved.getStatus().getErrorMessage()).isNull();
  }

  @Test
  void updateStatus_whenPaymentFoundWithError_savesErrorMessage() {
    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    PaymentProcessedEvent event =
        new PaymentProcessedEvent(paymentId, PaymentStatus.FAILED, null, "Bank timeout");
    paymentService.updateStatus(event);

    ArgumentCaptor<Payment> capturedPayment = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(capturedPayment.capture());
    Payment saved = capturedPayment.getValue();
    assertThat(saved.getStatus().getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(saved.getStatus().getErrorMessage()).isEqualTo("Bank timeout");
    assertThat(saved.getStatus().getTransactionId()).isNull();
  }

  @Test
  void updateStatus_whenPaymentNotFound_doesNotSave() {
    when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

    PaymentProcessedEvent event =
        new PaymentProcessedEvent(paymentId, PaymentStatus.SUCCESS, "TXN-999", null);
    paymentService.updateStatus(event);

    verify(paymentRepository, never()).save(any());
  }

  @Test
  void initiatePayment_success_savesPaymentAndOutboxEvent() throws JsonProcessingException {
    PaymentRequest request =
        new PaymentRequest(BigDecimal.valueOf(100), "USD", "ACC-REC-001", "ACC-SND-001");

    Payment mappedPayment = new Payment();
    mappedPayment.setId(paymentId);

    PaymentStatusEntity savedStatus =
        PaymentStatusEntity.builder().status(PaymentStatus.PENDING).build();
    mappedPayment.setStatus(savedStatus);

    when(paymentMapper.toEntity(request)).thenReturn(mappedPayment);
    when(paymentRepository.save(any())).thenReturn(mappedPayment);

    PaymentCreatedEvent createdEvent =
        new PaymentCreatedEvent(
            paymentId,
            BigDecimal.valueOf(100),
            "USD",
            "ACC-REC-001",
            "ACC-SND-001",
            null,
            PaymentStatus.PENDING);
    when(paymentMapper.toPaymentCreatedEvent(mappedPayment)).thenReturn(createdEvent);
    when(objectMapper.writeValueAsString(createdEvent))
        .thenReturn("{\"paymentId\":\"" + paymentId + "\"}");

    PaymentResponse expectedResponse =
        new PaymentResponse(paymentId, "PENDING", "Payment initiated successfully");
    when(paymentMapper.toResponse(mappedPayment, "Payment initiated successfully"))
        .thenReturn(expectedResponse);

    PaymentResponse response = paymentService.initiatePayment(request);

    assertThat(response).isEqualTo(expectedResponse);
    verify(paymentRepository).save(mappedPayment);
    verify(outboxRepository).save(any(OutboxEvent.class));

    ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxRepository).save(outboxCaptor.capture());
    OutboxEvent outbox = outboxCaptor.getValue();
    assertThat(outbox.getAggregateId()).isEqualTo(paymentId);
    assertThat(outbox.isProcessed()).isFalse();
  }

  @Test
  void initiatePayment_whenSerializationFails_throwsRuntimeException()
      throws JsonProcessingException {
    PaymentRequest request =
        new PaymentRequest(BigDecimal.valueOf(50), "EUR", "ACC-REC-002", "ACC-SND-002");

    Payment mappedPayment = new Payment();
    mappedPayment.setId(paymentId);
    mappedPayment.setStatus(PaymentStatusEntity.builder().status(PaymentStatus.PENDING).build());

    when(paymentMapper.toEntity(request)).thenReturn(mappedPayment);
    when(paymentRepository.save(any())).thenReturn(mappedPayment);

    PaymentCreatedEvent createdEvent =
        new PaymentCreatedEvent(
            paymentId,
            BigDecimal.valueOf(50),
            "EUR",
            "ACC-REC-002",
            "ACC-SND-002",
            null,
            PaymentStatus.PENDING);
    when(paymentMapper.toPaymentCreatedEvent(mappedPayment)).thenReturn(createdEvent);
    when(objectMapper.writeValueAsString(createdEvent))
        .thenThrow(new JsonProcessingException("fail") {});

    assertThatThrownBy(() -> paymentService.initiatePayment(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to initiate payment due to serialization error");

    verify(outboxRepository, never()).save(any());
  }
}
