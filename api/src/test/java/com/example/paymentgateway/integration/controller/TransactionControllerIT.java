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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
public class TransactionControllerIT extends IntegrationTestBase {

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
  @WithMockUser(username = "user1")
  void shouldTransferAndBeIdempotent() throws Exception {
    User user = new User();
    user.setEmail("sender@example.com");
    user.setName("Sender");
    userRepository.save(user);

    User user2 = new User();
    user2.setEmail("receiver@example.com");
    user2.setName("Receiver");
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

    long beforeCount = ledgerEntryRepository.count();

    TransferRequest request = new TransferRequest();
    request.setFromWalletId(from.getId());
    request.setToWalletId(to.getId());
    request.setAmount(new BigDecimal("10.00"));

    String body = objectMapper.writeValueAsString(request);

    mockMvc.perform(post("/transactions/transfer")
        .header("Idempotency-Key", "idem-123")
        .contentType("application/json")
        .content(body))
      .andExpect(status().isOk());

    mockMvc.perform(post("/transactions/transfer")
        .header("Idempotency-Key", "idem-123")
        .contentType("application/json")
        .content(body))
      .andExpect(status().isOk());

    long afterCount = ledgerEntryRepository.count();

    assertThat(afterCount).isEqualTo(beforeCount + 2);
  }

  @Test
  @WithMockUser(username = "user1")
  void shouldReturnConflictWhenIdempotencyKeyReusedWithDifferentPayload() throws Exception {
    User user = new User();
    user.setEmail("sender2@example.com");
    user.setName("Sender2");
    userRepository.save(user);

    User user2 = new User();
    user2.setEmail("receiver2@example.com");
    user2.setName("Receiver2");
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

    TransferRequest requestA = new TransferRequest();
    requestA.setFromWalletId(from.getId());
    requestA.setToWalletId(to.getId());
    requestA.setAmount(new BigDecimal("10.00"));

    TransferRequest requestB = new TransferRequest();
    requestB.setFromWalletId(from.getId());
    requestB.setToWalletId(to.getId());
    requestB.setAmount(new BigDecimal("11.00"));

    mockMvc.perform(post("/transactions/transfer")
        .header("Idempotency-Key", "idem-conflict-1")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(requestA)))
      .andExpect(status().isOk());

    mockMvc.perform(post("/transactions/transfer")
        .header("Idempotency-Key", "idem-conflict-1")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(requestB)))
      .andExpect(status().isConflict());
  }
}

