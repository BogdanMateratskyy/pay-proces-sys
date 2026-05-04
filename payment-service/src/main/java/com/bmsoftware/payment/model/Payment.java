package com.bmsoftware.payment.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments", schema = "payment")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Payment extends BaseEntity {

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false)
  private String recipientAccount;

  @Column(nullable = false)
  private String senderAccount;

  @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
  private PaymentStatusEntity status;
}
