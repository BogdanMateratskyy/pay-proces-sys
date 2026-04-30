package com.bmsoftware.payment.repository;

import com.bmsoftware.payment.model.OutboxEvent;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends BaseRepository<OutboxEvent> {
  List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
}
