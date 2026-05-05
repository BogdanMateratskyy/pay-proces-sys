package com.bmsoftware.processing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bmsoftware.shared.dto.BankPaymentResponse;
import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.bmsoftware.shared.dto.PaymentProcessedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {
      "${spring.kafka.consumer.payment-created-topic}",
      "${spring.kafka.consumer.payment-processed-topic}"
    },
    brokerProperties = {"listeners=PLAINTEXT://localhost:9094", "port=9094"})
@DirtiesContext
class PaymentEventListenerIT {

  @Autowired private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ConsumerFactory<String, String> consumerFactory;

  @MockitoBean private RoutingService routingService;

  @Value("${spring.kafka.consumer.payment-created-topic}")
  private String createdTopic;

  @Value("${spring.kafka.consumer.payment-processed-topic}")
  private String processedTopic;

  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;

  private KafkaConsumer<String, String> testConsumer;

  @BeforeEach
  void setUp() {
    testConsumer = (KafkaConsumer<String, String>) consumerFactory.createConsumer(groupId, "it");
    testConsumer.subscribe(Collections.singletonList(processedTopic));
    testConsumer.poll(Duration.ofMillis(100));
  }

  @AfterEach
  void tearDown() {
    testConsumer.close();
  }

  @Test
  void shouldPublishProcessedEventWithSuccessStatusWhenRoutingSucceeds() throws Exception {
    UUID paymentId = UUID.randomUUID();
    PaymentCreatedEvent event =
        new PaymentCreatedEvent(
            paymentId,
            new BigDecimal("200.00"),
            "USD",
            "RECP001",
            "SEND001",
            LocalDateTime.now(),
            PaymentStatus.PENDING);

    when(routingService.routePayment(any()))
        .thenReturn(new BankPaymentResponse(paymentId, PaymentStatus.SUCCESS, "TXN-IT-001", null));

    kafkaTemplate.send(createdTopic, paymentId.toString(), objectMapper.writeValueAsString(event));

    await()
        .atMost(15, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofMillis(500));
              assertThat(records.count()).isGreaterThan(0);
              ConsumerRecord<String, String> record = records.iterator().next();
              PaymentProcessedEvent processed =
                  objectMapper.readValue(record.value(), PaymentProcessedEvent.class);
              assertThat(processed.paymentId()).isEqualTo(paymentId);
              assertThat(processed.status()).isEqualTo(PaymentStatus.SUCCESS);
              assertThat(processed.transactionId()).isEqualTo("TXN-IT-001");
            });
  }

  @Test
  void shouldPublishProcessedEventWithFailedStatusWhenRoutingThrows() throws Exception {
    UUID paymentId = UUID.randomUUID();
    PaymentCreatedEvent event =
        new PaymentCreatedEvent(
            paymentId,
            new BigDecimal("500.00"),
            "EUR",
            "RECP002",
            "SEND002",
            LocalDateTime.now(),
            PaymentStatus.PENDING);

    when(routingService.routePayment(any())).thenThrow(new RuntimeException("Bank unavailable"));

    kafkaTemplate.send(createdTopic, paymentId.toString(), objectMapper.writeValueAsString(event));

    await()
        .atMost(15, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              ConsumerRecords<String, String> records =
                  testConsumer.poll(java.time.Duration.ofMillis(500));
              assertThat(records.count()).isGreaterThan(0);
              ConsumerRecord<String, String> record = records.iterator().next();
              PaymentProcessedEvent processed =
                  objectMapper.readValue(record.value(), PaymentProcessedEvent.class);
              assertThat(processed.paymentId()).isEqualTo(paymentId);
              assertThat(processed.status()).isEqualTo(PaymentStatus.FAILED);
              assertThat(processed.errorMessage()).isEqualTo("Bank unavailable");
            });
  }

  @Test
  void shouldNotPublishAnyEventWhenPayloadIsInvalidJson() {
    kafkaTemplate.send(createdTopic, "invalid-json-payload");

    // Wait briefly then assert nothing was published to payments.processed
    await()
        .during(3, TimeUnit.SECONDS)
        .atMost(4, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              ConsumerRecords<String, String> records =
                  testConsumer.poll(java.time.Duration.ofMillis(200));
              assertThat(records.count()).isZero();
            });
  }
}
