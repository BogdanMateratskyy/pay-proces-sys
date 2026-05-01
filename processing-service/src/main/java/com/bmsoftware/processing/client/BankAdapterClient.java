package com.bmsoftware.processing.client;

import com.bmsoftware.shared.dto.BankPaymentRequest;
import com.bmsoftware.shared.dto.BankPaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "bank-adapter-service", url = "${app.bank-adapter-url:http://localhost:8083}")
public interface BankAdapterClient {

  @PostMapping("/api/v1/banks/bank-a/process")
  BankPaymentResponse processBankA(@RequestBody BankPaymentRequest request);

  @PostMapping("/api/v1/banks/bank-b/process")
  BankPaymentResponse processBankB(@RequestBody BankPaymentRequest request);
}
