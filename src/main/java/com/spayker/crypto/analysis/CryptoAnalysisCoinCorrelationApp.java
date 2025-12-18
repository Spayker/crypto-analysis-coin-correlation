package com.spayker.crypto.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CryptoAnalysisCoinCorrelationApp {

  public static void main(String[] args) {
    SpringApplication.run(CryptoAnalysisCoinCorrelationApp.class);
  }

}