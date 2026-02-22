package com.example.paymentgateway.unit.service;

import com.example.paymentgateway.service.HmacSignatureService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HmacSignatureServiceTest {
  @Test
  void shouldSignAndVerifyPayload() {
    HmacSignatureService service = new HmacSignatureService();
    String payload = "{\"event\":\"PAYMENT_SUCCESS\"}";
    String secret = "secret-key-123";

    String signature = service.sign(payload, secret);

    assertThat(service.verify(signature, payload, secret)).isTrue();
    assertThat(service.verify(signature, payload + "x", secret)).isFalse();
  }
}
