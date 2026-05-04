package com.bmsoftware.payment.mapper;

import com.bmsoftware.payment.dto.PaymentRequest;
import com.bmsoftware.payment.dto.PaymentResponse;
import com.bmsoftware.payment.model.Payment;
import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

  Payment toEntity(PaymentRequest request);

  @Mapping(target = "paymentId", source = "payment.id")
  @Mapping(target = "status", source = "payment.status.status")
  PaymentResponse toResponse(Payment payment, String message);

  @Mapping(target = "paymentId", source = "payment.id")
  @Mapping(target = "status", source = "payment.status.status")
  PaymentCreatedEvent toPaymentCreatedEvent(Payment payment);
}
