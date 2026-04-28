package com.bmsoftware.payment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bmsoftware.payment.dto.PaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  private String validToken;
  private final String secret = "mysecretkeymysecretkeymysecretkeymysecretkey";

  @BeforeEach
  void setUp() {
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
  void shouldInitiatePaymentSuccessfully() throws Exception {
    PaymentRequest request =
        new PaymentRequest(new BigDecimal("100.00"), "USD", "RECP123", "SENDER123");

    mockMvc
        .perform(
            post("/api/v1/payments")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paymentId").exists())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.message").value("Payment initiated successfully"));
  }

  @Test
  void shouldReturnForbiddenWhenNoToken() throws Exception {
    PaymentRequest request =
        new PaymentRequest(new BigDecimal("100.00"), "USD", "RECP123", "SENDER123");

    mockMvc
        .perform(
            post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnBadRequestWhenAmountIsNegative() throws Exception {
    PaymentRequest request =
        new PaymentRequest(new BigDecimal("-100.00"), "USD", "RECP123", "SENDER123");

    mockMvc
        .perform(
            post("/api/v1/payments")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequestWhenCurrencyIsInvalid() throws Exception {
    PaymentRequest request =
        new PaymentRequest(new BigDecimal("100.00"), "US", "RECP123", "SENDER123");

    mockMvc
        .perform(
            post("/api/v1/payments")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequestWhenRecipientIsEmpty() throws Exception {
    PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "USD", "", "SENDER123");

    mockMvc
        .perform(
            post("/api/v1/payments")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
