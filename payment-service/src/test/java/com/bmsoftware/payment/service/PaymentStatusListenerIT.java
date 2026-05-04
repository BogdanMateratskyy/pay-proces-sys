package com.bmsoftware.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bmsoftware.payment.model.Payment;
import com.bmsoftware.payment.model.PaymentStatusEntity;
import com.bmsoftware.payment.repository.PaymentRepository;
import com.bmsoftware.shared.dto.PaymentProcessedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"${spring.kafka.payment-processed-topic}"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"})
@DirtiesContext
class PaymentStatusListenerIT {

  @Autowired private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired private PaymentRepository paymentRepository;

  @Autowired private ObjectMapper objectMapper;

  @Value("${spring.kafka.payment-processed-topic}")
  private String topic;

  private Payment savedPayment;

  @BeforeEach
  void setUp() {
    paymentRepository.deleteAll();
    PaymentStatusEntity initialStatus =
        PaymentStatusEntity.builder().status(PaymentStatus.PENDING).build();
    Payment payment =
        Payment.builder()
            .amount(new BigDecimal("250.00"))
            .currency("USD")
            .recipientAccount("RECP001")
            .senderAccount("SEND001")
            .status(initialStatus)
            .build();
    initialStatus.setPayment(payment);
    savedPayment = paymentRepository.save(payment);
  }

  @Test
  void shouldUpdatePaymentStatusToSuccessWhenEventReceived() throws Exception {
    PaymentProcessedEvent event =
        new PaymentProcessedEvent(savedPayment.getId(), PaymentStatus.SUCCESS, "TXN-001", null);
    String payload = objectMapper.writeValueAsString(event);

    kafkaTemplate.send(topic, payload);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Payment updated = paymentRepository.findById(savedPayment.getId()).orElseThrow();
              assertThat(updated.getStatus().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
              assertThat(updated.getStatus().getTransactionId()).isEqualTo("TXN-001");
            });
  }

  @Test
  void shouldUpdatePaymentStatusToFailedWhenEventReceived() throws Exception {
    PaymentProcessedEvent event =
        new PaymentProcessedEvent(
            savedPayment.getId(), PaymentStatus.FAILED, null, "Insufficient funds");
    String payload = objectMapper.writeValueAsString(event);

    kafkaTemplate.send(topic, payload);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Payment updated = paymentRepository.findById(savedPayment.getId()).orElseThrow();
              assertThat(updated.getStatus().getStatus()).isEqualTo(PaymentStatus.FAILED);
              assertThat(updated.getStatus().getErrorMessage()).isEqualTo("Insufficient funds");
            });
  }

  @Test
  void shouldNotUpdateStatusWhenPaymentIdDoesNotExist() throws Exception {
    UUID nonExistentId = UUID.randomUUID();
    PaymentProcessedEvent event =
        new PaymentProcessedEvent(nonExistentId, PaymentStatus.SUCCESS, "TXN-999", null);
    String payload = objectMapper.writeValueAsString(event);

    kafkaTemplate.send(topic, payload);

    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Payment original = paymentRepository.findById(savedPayment.getId()).orElseThrow();
              assertThat(original.getStatus().getStatus()).isEqualTo(PaymentStatus.PENDING);
            });
  }

  @Test
  void shouldHandleInvalidJsonPayloadGracefully() {
    kafkaTemplate.send(topic, "invalid-json-payload");

    // Listener should swallow the error; original payment must remain unchanged
    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Payment original = paymentRepository.findById(savedPayment.getId()).orElseThrow();
              assertThat(original.getStatus().getStatus()).isEqualTo(PaymentStatus.PENDING);
            });
  }
}
