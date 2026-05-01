package com.bmsoftware.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class ProcessingServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(ProcessingServiceApplication.class, args);
  }
}
