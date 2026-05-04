package com.bmsoftware.payment.service;

import com.bmsoftware.payment.model.Payment;
import com.bmsoftware.payment.model.PaymentAuditLog;
import com.bmsoftware.payment.repository.PaymentAuditLogRepository;
import com.bmsoftware.shared.dto.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentAuditLogService {

  private final PaymentAuditLogRepository auditLogRepository;

  public void saveAuditLog(
      Payment payment,
      PaymentStatus previousStatus,
      PaymentStatus newStatus,
      String transactionId,
      String errorMessage) {
    auditLogRepository.save(
        PaymentAuditLog.builder()
            .payment(payment)
            .previousStatus(previousStatus)
            .newStatus(newStatus)
            .transactionId(transactionId)
            .errorMessage(errorMessage)
            .build());
  }
}
