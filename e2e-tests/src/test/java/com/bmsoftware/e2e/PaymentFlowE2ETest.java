package com.bmsoftware.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bmsoftware.payment.PaymentServiceApplication;
import com.bmsoftware.payment.dto.PaymentRequest;
import com.bmsoftware.payment.model.Payment;
import com.bmsoftware.payment.repository.PaymentAuditLogRepository;
import com.bmsoftware.payment.repository.PaymentRepository;
import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.bmsoftware.shared.dto.PaymentProcessedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = PaymentServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"${spring.kafka.payment-processed-topic}", "payments.created"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"})
@DirtiesContext
class PaymentFlowE2ETest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private PaymentAuditLogRepository paymentAuditLogRepository;
  @Autowired private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired private PaymentCreatedEventCaptor eventCaptor;

  @Value("${spring.kafka.payment-processed-topic}")
  private String processedTopic;

  private String validToken;
  private final String secret = "mysecretkeymysecretkeymysecretkeymysecretkey";

  @BeforeEach
  void setUp() {
    paymentAuditLogRepository.deleteAll();
    paymentRepository.deleteAll();
    eventCaptor.reset();

    validToken =
        Jwts.builder()
            .subject("testuser")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(
                Keys.hmacShaKeyFor(
                    Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8))))
            .compact();
  }

  @Test
  void shouldCompleteFullPaymentFlowFromInitiationToSuccess() throws Exception {
    PaymentRequest request =
        new PaymentRequest(new BigDecimal("150.00"), "USD", "RECP-E2E-001", "SEND-E2E-001");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/payments")
                    .header("Authorization", "Bearer " + validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").exists())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    String paymentIdStr = objectMapper.readTree(responseBody).get("paymentId").asText();
    UUID paymentId = UUID.fromString(paymentIdStr);

    Payment savedPayment = paymentRepository.findById(paymentId).orElseThrow();
    assertThat(savedPayment.getStatus().getStatus()).isEqualTo(PaymentStatus.PENDING);

    await().atMost(15, TimeUnit.SECONDS).until(() -> eventCaptor.getCapturedEvent() != null);

    PaymentCreatedEvent createdEvent = eventCaptor.getCapturedEvent();
    assertThat(createdEvent.paymentId()).isEqualTo(paymentId);
    assertThat(createdEvent.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
    assertThat(createdEvent.currency()).isEqualTo("USD");

    PaymentProcessedEvent processedEvent =
        new PaymentProcessedEvent(paymentId, PaymentStatus.SUCCESS, "TXN-E2E-001", null);
    kafkaTemplate.send(
        processedTopic, paymentId.toString(), objectMapper.writeValueAsString(processedEvent));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Payment updated = paymentRepository.findById(paymentId).orElseThrow();
              assertThat(updated.getStatus().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
              assertThat(updated.getStatus().getTransactionId()).isEqualTo("TXN-E2E-001");
            });

    mockMvc
        .perform(
            get("/api/v1/payments/{id}", paymentId).header("Authorization", "Bearer " + validToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
        .andExpect(jsonPath("$.status").value("SUCCESS"));

    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> assertThat(paymentAuditLogRepository.findAll()).hasSizeGreaterThanOrEqualTo(2));
  }

  @Test
  void shouldCompleteFullPaymentFlowFromInitiationToFailed() throws Exception {
    PaymentRequest request =
        new PaymentRequest(new BigDecimal("500.00"), "EUR", "RECP-E2E-002", "SEND-E2E-002");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/payments")
                    .header("Authorization", "Bearer " + validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();

    String paymentIdStr =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("paymentId").asText();
    UUID paymentId = UUID.fromString(paymentIdStr);

    await().atMost(15, TimeUnit.SECONDS).until(() -> eventCaptor.getCapturedEvent() != null);

    PaymentProcessedEvent failedEvent =
        new PaymentProcessedEvent(paymentId, PaymentStatus.FAILED, null, "Insufficient funds");
    kafkaTemplate.send(
        processedTopic, paymentId.toString(), objectMapper.writeValueAsString(failedEvent));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Payment updated = paymentRepository.findById(paymentId).orElseThrow();
              assertThat(updated.getStatus().getStatus()).isEqualTo(PaymentStatus.FAILED);
              assertThat(updated.getStatus().getErrorMessage()).isEqualTo("Insufficient funds");
            });

    mockMvc
        .perform(
            get("/api/v1/payments/{id}", paymentId).header("Authorization", "Bearer " + validToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"));
  }

  @Test
  void shouldReturnNotFoundForUnknownPaymentId() throws Exception {
    UUID unknownId = UUID.randomUUID();
    mockMvc
        .perform(
            get("/api/v1/payments/{id}", unknownId).header("Authorization", "Bearer " + validToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldRejectPaymentInitiationWithoutAuthentication() throws Exception {
    PaymentRequest request =
        new PaymentRequest(new BigDecimal("100.00"), "USD", "RECP-E2E-003", "SEND-E2E-003");
    mockMvc
        .perform(
            post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }
}
