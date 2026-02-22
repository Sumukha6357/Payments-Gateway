package com.example.paymentgateway.integration.controller;

import com.example.paymentgateway.domain.entity.LedgerEntry;
import com.example.paymentgateway.domain.entity.User;
import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.domain.enumtype.LedgerEntryType;
import com.example.paymentgateway.dto.TransferRequest;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@AutoConfigureMockMvc(addFilters = false)
public class RateLimitIT extends IntegrationTestBase {

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
  @WithMockUser(username = "rateuser")
  void shouldEnforceRateLimit() throws Exception {
    User user = new User();
    user.setEmail("rate@example.com");
    user.setName("Rate User");
    userRepository.save(user);

    User user2 = new User();
    user2.setEmail("rate2@example.com");
    user2.setName("Rate User2");
    userRepository.save(user2);

    Wallet from = new Wallet();
    from.setUser(user);
    from.setCurrency("USD");
    walletRepository.save(from);

    Wallet to = new Wallet();
    to.setUser(user2);
    to.setCurrency("USD");
    walletRepository.save(to);

    LedgerEntry credit = new LedgerEntry();
    credit.setWallet(from);
    credit.setType(LedgerEntryType.CREDIT);
    credit.setAmount(new BigDecimal("100.00"));
    credit.setReferenceId(UUID.randomUUID());
    ledgerEntryRepository.save(credit);

    TransferRequest request = new TransferRequest();
    request.setFromWalletId(from.getId());
    request.setToWalletId(to.getId());
    request.setAmount(new BigDecimal("1.00"));

    String body = objectMapper.writeValueAsString(request);

    int[] statuses = new int[6];
    for (int i = 0; i < 6; i++) {
      statuses[i] = mockMvc.perform(post("/transactions/transfer")
          .header("Idempotency-Key", "idem-rate-" + i)
          .contentType("application/json")
          .content(body))
        .andReturn()
        .getResponse()
        .getStatus();
    }

    assertThat(statuses[0]).isEqualTo(200);
    assertThat(statuses[1]).isEqualTo(200);
    assertThat(statuses[2]).isEqualTo(200);
    assertThat(statuses[3]).isEqualTo(200);
    assertThat(statuses[4]).isEqualTo(200);
    assertThat(statuses[5]).isEqualTo(429);

    String responseBody = mockMvc.perform(post("/transactions/transfer")
        .header("Idempotency-Key", "idem-rate-6")
        .contentType("application/json")
        .content(body))
      .andReturn()
      .getResponse()
      .getContentAsString();

    JsonNode json = objectMapper.readTree(responseBody);
    assertThat(json.get("errorCode").asText()).isEqualTo("RATE_LIMITED");
  }
}

