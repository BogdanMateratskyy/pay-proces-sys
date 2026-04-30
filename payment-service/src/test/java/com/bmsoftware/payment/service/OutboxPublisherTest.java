package com.bmsoftware.payment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bmsoftware.payment.model.OutboxEvent;
import com.bmsoftware.payment.repository.OutboxRepository;
import com.bmsoftware.shared.dto.EventType;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

  @Mock private OutboxRepository outboxRepository;

  @Mock private KafkaTemplate<String, String> kafkaTemplate;

  @InjectMocks private OutboxPublisher outboxPublisher;

  private OutboxEvent event;

  @BeforeEach
  void setUp() {
    event =
        OutboxEvent.builder()
            .aggregateId(UUID.randomUUID())
            .eventType(EventType.PAYMENT_CREATED)
            .payload("{\"id\":\"123\"}")
            .processed(false)
            .build();
    event.setId(UUID.randomUUID());
  }

  @Test
  void publishEvents_WhenEventsExist_ShouldPublishAndMarkAsProcessed() {
    when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));

    CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
    when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

    outboxPublisher.publishEvents();

    verify(outboxRepository).findByProcessedFalseOrderByCreatedAtAsc();
    verify(kafkaTemplate).send(eq("payments.created"), anyString(), eq(event.getPayload()));
    verify(outboxRepository).save(event);
    Assertions.assertTrue(event.isProcessed());
  }

  @Test
  void publishEvents_WhenNoEvents_ShouldDoNothing() {
    when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
        .thenReturn(Collections.emptyList());

    outboxPublisher.publishEvents();

    verify(outboxRepository).findByProcessedFalseOrderByCreatedAtAsc();
    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    verify(outboxRepository, never()).save(any());
  }

  @Test
  void publishEvents_WhenKafkaFails_ShouldStillMarkAsProcessed() {
    // In current implementation, it marks as processed BEFORE completion callback result.
    when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));

    CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException("Kafka error"));

    when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

    outboxPublisher.publishEvents();

    verify(kafkaTemplate).send(anyString(), anyString(), anyString());
    verify(outboxRepository).save(event);
    assert event.isProcessed();
  }

  @Test
  void publishEvents_WhenExceptionOccursDuringProcessing_ShouldContinueWithNext() {
    OutboxEvent event2 =
        OutboxEvent.builder()
            .aggregateId(UUID.randomUUID())
            .eventType(EventType.PAYMENT_COMPLETED)
            .payload("{}")
            .processed(false)
            .build();
    event2.setId(UUID.randomUUID());

    when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
        .thenReturn(List.of(event, event2));

    // Fail first event processing by throwing exception in getTopicForEvent indirectly
    // (though getTopicForEvent is private, let's trigger it via some other way or just mock send to
    // throw)
    when(kafkaTemplate.send(eq("payments.created"), anyString(), anyString()))
        .thenThrow(new RuntimeException("Internal error"));

    CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
    when(kafkaTemplate.send(eq("payments.completed"), anyString(), anyString())).thenReturn(future);

    outboxPublisher.publishEvents();

    verify(kafkaTemplate, times(2)).send(anyString(), anyString(), anyString());
    verify(outboxRepository).save(event2);
    Assertions.assertTrue(event2.isProcessed());
    // event 1 failed before save
    verify(outboxRepository, never()).save(event);
  }
}
