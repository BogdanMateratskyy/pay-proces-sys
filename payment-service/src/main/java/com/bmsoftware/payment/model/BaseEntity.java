package com.bmsoftware.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Data
@MappedSuperclass
public class BaseEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  private UUID id;

  @CreationTimestamp
  @Column(nullable = false)
  private LocalDateTime createdAt;
}
