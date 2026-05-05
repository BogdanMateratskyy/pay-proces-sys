package com.bmsoftware.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bmsoftware.payment.PaymentServiceApplication;
import com.bmsoftware.payment.dto.PaymentRequest;
import com.bmsoftware.payment.repository.PaymentAuditLogRepository;
import com.bmsoftware.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = PaymentServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"payments.created"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9094", "port=9094"})
@DirtiesContext
class RateLimitLoadTest {

  private static final int CONCURRENT_REQUESTS = 50;
  private static final String SECRET = "mysecretkeymysecretkeymysecretkeymysecretkey";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private PaymentAuditLogRepository paymentAuditLogRepository;

  private String validToken;

  @BeforeEach
  void setUp() {
    paymentAuditLogRepository.deleteAll();
    paymentRepository.deleteAll();

    validToken =
        Jwts.builder()
            .subject("loadtestuser")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(
                Keys.hmacShaKeyFor(
                    Base64.getDecoder().decode(SECRET.getBytes(StandardCharsets.UTF_8))))
            .compact();
  }

  @Test
  void shouldHandleConcurrentRequestsWithoutSystemFailure() throws Exception {
    List<Future<Integer>> futures;
    try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
      List<Callable<Integer>> tasks = preparePaymentTasks();

      futures = executor.invokeAll(tasks);
      executor.shutdown();
    }

    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger errorCount = new AtomicInteger();

    for (Future<Integer> future : futures) {
      int status = future.get();
      if (status == 200) {
        successCount.incrementAndGet();
      } else {
        errorCount.incrementAndGet();
      }
    }

    // All requests should succeed at the service level (rate limiting is enforced at API Gateway)
    assertThat(successCount.get()).isEqualTo(CONCURRENT_REQUESTS);
    assertThat(errorCount.get()).isZero();
    assertThat(paymentRepository.count()).isEqualTo(CONCURRENT_REQUESTS);
  }

  private @NonNull List<Callable<Integer>> preparePaymentTasks() {
    List<Callable<Integer>> tasks = new ArrayList<>();

    for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
      final int index = i;
      tasks.add(
          () -> {
            PaymentRequest request =
                new PaymentRequest(
                    new BigDecimal("10.00"), "USD", "RECP-LOAD-" + index, "SEND-LOAD-" + index);
            MockHttpServletResponse response =
                mockMvc
                    .perform(
                        post("/api/v1/payments")
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();
            return response.getStatus();
          });
    }
    return tasks;
  }

  @Test
  void shouldRejectAllRequestsWithoutValidToken() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(5);
    List<Callable<Integer>> tasks = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      final int index = i;
      tasks.add(
          () -> {
            PaymentRequest request =
                new PaymentRequest(
                    new BigDecimal("10.00"), "USD", "RECP-UNAUTH-" + index, "SEND-UNAUTH-" + index);
            MockHttpServletResponse response =
                mockMvc
                    .perform(
                        post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();
            return response.getStatus();
          });
    }

    List<Future<Integer>> futures = executor.invokeAll(tasks);
    executor.shutdown();

    for (Future<Integer> future : futures) {
      assertThat(future.get()).isEqualTo(403);
    }

    assertThat(paymentRepository.count()).isZero();
  }

  @Test
  void shouldMaintainDataIntegrityUnderLoad() throws Exception {
    int requestCount = 20;
    ExecutorService executor = Executors.newFixedThreadPool(5);
    List<Callable<Integer>> tasks = new ArrayList<>();

    for (int i = 0; i < requestCount; i++) {
      final int index = i;
      tasks.add(
          () -> {
            PaymentRequest request =
                new PaymentRequest(
                    new BigDecimal("25.00"), "EUR", "RECP-INT-" + index, "SEND-INT-" + index);
            MockHttpServletResponse response =
                mockMvc
                    .perform(
                        post("/api/v1/payments")
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();
            return response.getStatus();
          });
    }

    List<Future<Integer>> futures = executor.invokeAll(tasks);
    executor.shutdown();

    long successCount = futures.stream().map(f -> {
      try { return f.get(); } catch (Exception e) { return -1; }
    }).filter(s -> s == 200).count();

    assertThat(successCount).isEqualTo(requestCount);
    assertThat(paymentRepository.count()).isEqualTo(requestCount);
  }
}
