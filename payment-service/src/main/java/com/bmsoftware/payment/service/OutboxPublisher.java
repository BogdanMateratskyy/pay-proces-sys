package com.bmsoftware.payment.service;

import com.bmsoftware.payment.model.OutboxEvent;
import com.bmsoftware.payment.repository.OutboxRepository;
import com.bmsoftware.shared.dto.EventType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

  private final OutboxRepository outboxRepository;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void publishEvents() {
    List<OutboxEvent> events = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();

    if (events.isEmpty()) {
      return;
    }

    log.info("Found {} unprocessed outbox events", events.size());

    for (OutboxEvent event : events) {
      try {
        String topic = getTopicForEvent(event.getEventType());
        kafkaTemplate
            .send(topic, event.getAggregateId().toString(), event.getPayload())
            .whenComplete(
                (result, ex) -> {
                  if (ex == null) {
                    log.info("Successfully published event {} to topic {}", event.getId(), topic);
                  } else {
                    log.error("Failed to publish event {} to topic {}", event.getId(), topic, ex);
                  }
                });

        // For simplicity in this example, we mark as processed immediately after sending to Kafka.
        // In a more robust implementation, we might wait for the completion callback,
        // but that's trickier with @Transactional and simple Outbox.
        event.setProcessed(true);
        outboxRepository.save(event);

      } catch (Exception e) {
        log.error("Error processing outbox event {}", event.getId(), e);
      }
    }
  }

  private String getTopicForEvent(EventType eventType) {
    return switch (eventType) {
      case EventType.PAYMENT_CREATED -> "payments.created";
    };
  }
}
