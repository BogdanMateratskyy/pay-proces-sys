package com.bmsoftware.payment.mapper;

import com.bmsoftware.payment.dto.PaymentRequest;
import com.bmsoftware.payment.dto.PaymentResponse;
import com.bmsoftware.payment.model.Payment;
import com.bmsoftware.shared.dto.PaymentCreatedEvent;
import com.bmsoftware.shared.dto.PaymentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

  Payment toEntity(PaymentRequest request, PaymentStatus status);

  @Mapping(target = "paymentId", source = "payment.id")
  PaymentResponse toResponse(Payment payment, String message);

  @Mapping(target = "paymentId", source = "payment.id")
  PaymentCreatedEvent toPaymentCreatedEvent(Payment payment);
}
