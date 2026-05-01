package com.bmsoftware.processing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.bmsoftware.processing.client.BankAdapterClient;
import com.bmsoftware.shared.dto.BankPaymentResponse;
import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoutingServiceResilienceTest {

  @Autowired private RoutingService routingService;

  @MockitoBean private BankAdapterClient bankAdapterClient;

  @Test
  void testRetryAndFallback() {
    UUID paymentId = UUID.randomUUID();
    PaymentCreatedEvent event =
        new PaymentCreatedEvent(
            paymentId,
            new BigDecimal("100"),
            "USD",
            "acc1",
            "acc2",
            LocalDateTime.now(),
            PaymentStatus.PENDING);

    RuntimeException serviceUnavailable = new RuntimeException("Service Unavailable");
    when(bankAdapterClient.processBankA(any())).thenThrow(serviceUnavailable);

    BankPaymentResponse response = routingService.routePayment(event);

    verify(bankAdapterClient, atLeast(1)).processBankA(any());
    // We check if it returned FAILED status from fallback at least
    assertEquals(PaymentStatus.FAILED, response.status());
    assertEquals("Bank integration unavailable: Service Unavailable", response.errorMessage());
  }
}
