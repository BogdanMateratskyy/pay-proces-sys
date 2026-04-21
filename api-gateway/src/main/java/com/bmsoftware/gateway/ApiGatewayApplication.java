package com.bmsoftware.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bmsoftware")
public class ApiGatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
