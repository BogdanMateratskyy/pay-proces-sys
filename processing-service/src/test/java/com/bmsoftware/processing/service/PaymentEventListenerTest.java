package com.bmsoftware.processing.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bmsoftware.shared.dto.BankPaymentResponse;
import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private RoutingService routingService;
  @Mock private KafkaTemplate<String, String> kafkaTemplate;

  @Value("${spring.kafka.consumer.payment-processed-topic}")
  public String paymentsProcessed;

  @InjectMocks private PaymentEventListener listener;

  private PaymentCreatedEvent event;
  private final UUID paymentId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    event =
        new PaymentCreatedEvent(
            paymentId,
            new BigDecimal("150.00"),
            "USD",
            "RECP001",
            "SEND001",
            LocalDateTime.now(),
            PaymentStatus.PENDING);
  }

  @Test
  void shouldRoutePaymentAndPublishProcessedEventOnSuccess() throws Exception {
    String payload = "{\"paymentId\":\"" + paymentId + "\"}";
    BankPaymentResponse bankResponse =
        new BankPaymentResponse(paymentId, PaymentStatus.SUCCESS, "TXN-123", null);

    when(objectMapper.readValue(payload, PaymentCreatedEvent.class)).thenReturn(event);
    when(routingService.routePayment(event)).thenReturn(bankResponse);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"processed\":true}");

    listener.handlePaymentCreated(payload);

    verify(routingService).routePayment(event);
    verify(kafkaTemplate).send(eq(paymentsProcessed), eq(paymentId.toString()), anyString());
  }

  @Test
  void shouldPublishFailedEventWhenRoutingThrowsException() throws Exception {
    String payload = "{\"paymentId\":\"" + paymentId + "\"}";

    when(objectMapper.readValue(payload, PaymentCreatedEvent.class)).thenReturn(event);
    when(routingService.routePayment(event)).thenThrow(new RuntimeException("Bank unavailable"));
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":\"FAILED\"}");

    listener.handlePaymentCreated(payload);

    verify(kafkaTemplate).send(eq(paymentsProcessed), eq(paymentId.toString()), anyString());
  }

  @Test
  void shouldSwallowErrorWhenPayloadIsInvalidJson() throws Exception {
    String invalidPayload = "not-valid-json";

    when(objectMapper.readValue(invalidPayload, PaymentCreatedEvent.class))
        .thenThrow(new RuntimeException("Invalid JSON"));

    listener.handlePaymentCreated(invalidPayload);

    verifyNoInteractions(routingService);
    verifyNoInteractions(kafkaTemplate);
  }

  @Test
  void shouldSwallowErrorWhenPublishingProcessedEventFails() throws Exception {
    String payload = "{\"paymentId\":\"" + paymentId + "\"}";
    BankPaymentResponse bankResponse =
        new BankPaymentResponse(paymentId, PaymentStatus.SUCCESS, "TXN-456", null);

    when(objectMapper.readValue(payload, PaymentCreatedEvent.class)).thenReturn(event);
    when(routingService.routePayment(event)).thenReturn(bankResponse);
    when(objectMapper.writeValueAsString(any()))
        .thenThrow(new RuntimeException("Serialization error"));

    listener.handlePaymentCreated(payload);

    verifyNoInteractions(kafkaTemplate);
  }
}
