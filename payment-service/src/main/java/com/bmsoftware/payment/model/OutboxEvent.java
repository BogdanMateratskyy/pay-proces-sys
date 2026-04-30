package com.bmsoftware.payment.model;

import com.bmsoftware.shared.dto.AggregateType;
import com.bmsoftware.shared.dto.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "outbox", schema = "payment")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OutboxEvent extends BaseEntity {

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private AggregateType aggregateType;

  @Column(nullable = false)
  private UUID aggregateId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EventType eventType;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String payload;

  @Column @UpdateTimestamp private LocalDateTime processedAt;

  @Column(nullable = false)
  private boolean processed;
}
