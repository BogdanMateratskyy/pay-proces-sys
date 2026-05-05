package com.bmsoftware.e2e;

import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCreatedEventCaptor {

  private volatile PaymentCreatedEvent capturedEvent;

  @Autowired private ObjectMapper objectMapper;

  @KafkaListener(topics = "payments.created", groupId = "e2e-captor-group")
  public void capture(ConsumerRecord<String, String> record) {
    try {
      if (capturedEvent == null) {
        capturedEvent = objectMapper.readValue(record.value(), PaymentCreatedEvent.class);
      }
    } catch (Exception ignored) {
    }
  }

  public PaymentCreatedEvent getCapturedEvent() {
    return capturedEvent;
  }

  public void reset() {
    capturedEvent = null;
  }
}
