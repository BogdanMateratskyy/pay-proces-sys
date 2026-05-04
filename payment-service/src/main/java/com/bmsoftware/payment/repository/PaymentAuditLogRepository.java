package com.bmsoftware.payment.repository;

import com.bmsoftware.payment.model.PaymentAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentAuditLogRepository extends BaseRepository<PaymentAuditLog> {

  @Query("SELECT a FROM PaymentAuditLog a WHERE a.payment.id = :paymentId ORDER BY a.createdAt ASC")
  List<PaymentAuditLog> findByPaymentIdOrderByCreatedAtAsc(@Param("paymentId") UUID paymentId);
}
