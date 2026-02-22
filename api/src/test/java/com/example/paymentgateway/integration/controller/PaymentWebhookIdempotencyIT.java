package com.example.paymentgateway.integration.controller;

import com.example.paymentgateway.domain.entity.User;
import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.dto.PaymentRequest;
import com.example.paymentgateway.integration.support.IntegrationTestBase;
import com.example.paymentgateway.repository.LedgerEntryRepository;
import com.example.paymentgateway.repository.UserRepository;
import com.example.paymentgateway.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
public class PaymentWebhookIdempotencyIT extends IntegrationTestBase {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WalletRepository walletRepository;

  @Autowired
  private LedgerEntryRepository ledgerEntryRepository;

  @Test
  @WithMockUser(username = "payuser")
  void shouldHandleWebhookIdempotently() throws Exception {
    User user = new User();
    user.setEmail("pay@example.com");
    user.setName("Pay User");
    userRepository.save(user);

    Wallet wallet = new Wallet();
    wallet.setUser(user);
    wallet.setCurrency("USD");
    walletRepository.save(wallet);

    PaymentRequest request = new PaymentRequest();
    request.setWalletId(wallet.getId());
    request.setAmount(new BigDecimal("25.00"));

    String paymentResponse = mockMvc.perform(post("/payments")
        .header("Idempotency-Key", "idem-payment-1")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    JsonNode paymentJson = objectMapper.readTree(paymentResponse);
    String externalReference = paymentJson.get("externalReference").asText();

    String webhookBody = "{\"externalReference\":\"" + externalReference + "\",\"status\":\"SUCCESS\",\"amount\":25.00}";
    String timestamp = OffsetDateTime.now().toString();
    String signature = sign(externalReference + "|SUCCESS|25.00|" + timestamp, "test-webhook-secret-test-webhook-secret");

    long before = ledgerEntryRepository.count();

    mockMvc.perform(post("/payments/webhook")
        .header("X-Signature", signature)
        .header("X-Timestamp", timestamp)
        .header("X-Event-Id", "evt-1")
        .contentType("application/json")
        .content(webhookBody))
      .andExpect(status().isOk());

    mockMvc.perform(post("/payments/webhook")
        .header("X-Signature", signature)
        .header("X-Timestamp", timestamp)
        .header("X-Event-Id", "evt-1")
        .contentType("application/json")
        .content(webhookBody))
      .andExpect(status().isBadRequest());

    long after = ledgerEntryRepository.count();

    assertThat(after).isEqualTo(before + 1);
  }

  private String sign(String payload, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : raw) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}

