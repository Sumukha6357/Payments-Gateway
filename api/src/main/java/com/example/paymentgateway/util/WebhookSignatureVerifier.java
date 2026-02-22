package com.example.paymentgateway.util;

import com.example.paymentgateway.dto.PaymentWebhookRequest;
import com.example.paymentgateway.service.HmacSignatureService;
import com.example.paymentgateway.security.SecretValueResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebhookSignatureVerifier {
  private final String secret;
  private final HmacSignatureService hmacSignatureService;

  public WebhookSignatureVerifier(@Value("${payments.webhook.secret}") String secret,
                                  HmacSignatureService hmacSignatureService) {
    this.secret = SecretValueResolver.resolve("WEBHOOK_SECRET", secret);
    this.hmacSignatureService = hmacSignatureService;
  }

  public boolean isValid(String signature, PaymentWebhookRequest request, String timestamp) {
    if (signature == null || signature.isBlank()) {
      return false;
    }
    String payload = request.getExternalReference() + "|" + request.getStatus() + "|" + request.getAmount() + "|" + timestamp;
    return hmacSignatureService.verify(signature, payload, secret);
  }
}
