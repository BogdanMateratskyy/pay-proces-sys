package com.bmsoftware.payment.service;

import com.bmsoftware.payment.model.OutboxEvent;
import com.bmsoftware.payment.repository.OutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

  private final OutboxRepository outboxRepository;

  public OutboxEvent save(OutboxEvent event) {
    return outboxRepository.save(event);
  }

  public List<OutboxEvent> findUnprocessed() {
    return outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();
  }
}
