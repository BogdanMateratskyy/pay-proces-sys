package com.bmsoftware.payment.service;

import com.bmsoftware.payment.dto.PaymentRequest;
import com.bmsoftware.payment.dto.PaymentResponse;
import com.bmsoftware.payment.mapper.PaymentMapper;
import com.bmsoftware.payment.model.Payment;
import com.bmsoftware.payment.repository.PaymentRepository;
import com.bmsoftware.shared.dto.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;

  @Transactional
  public PaymentResponse initiatePayment(PaymentRequest request) {
    log.info("Initiating payment for amount: {} {}", request.amount(), request.currency());

    Payment payment = paymentMapper.toEntity(request, PaymentStatus.PENDING);
    Payment savedPayment = paymentRepository.save(payment);

    log.info(
        "Payment saved with ID: {} and status: {}", savedPayment.getId(), savedPayment.getStatus());

    return paymentMapper.toResponse(savedPayment, "Payment initiated successfully");
  }
}
