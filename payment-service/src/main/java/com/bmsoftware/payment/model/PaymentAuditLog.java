package com.bmsoftware.payment.model;

import com.bmsoftware.shared.dto.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_audit_log", schema = "payment")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PaymentAuditLog extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_id", nullable = false)
  private Payment payment;

  @Column
  @Enumerated(EnumType.STRING)
  private PaymentStatus previousStatus;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PaymentStatus newStatus;

  @Column private String transactionId;

  @Column(columnDefinition = "TEXT")
  private String errorMessage;
}
