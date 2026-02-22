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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@AutoConfigureMockMvc(addFilters = false)
public class ConcurrentTransferIT extends IntegrationTestBase {

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
  void concurrentTransfersShouldNotOverdraw() throws Exception {
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
    credit.setAmount(new BigDecimal("1000.00"));
    credit.setReferenceId(UUID.randomUUID());
    ledgerEntryRepository.save(credit);

    TransferRequest request = new TransferRequest();
    request.setFromWalletId(from.getId());
    request.setToWalletId(to.getId());
    request.setAmount(new BigDecimal("200.00"));

    String body = objectMapper.writeValueAsString(request);

    int threads = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    AtomicInteger successCount = new AtomicInteger();

    for (int i = 0; i < threads; i++) {
      int index = i;
      executor.submit(() -> {
        try {
          int status = mockMvc.perform(post("/transactions/transfer")
              .header("Idempotency-Key", "idem-concurrent-" + index)
              .contentType("application/json")
              .content(body))
            .andReturn()
            .getResponse()
            .getStatus();
          if (status == 200) {
            successCount.incrementAndGet();
          }
        } catch (Exception ignored) {
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await(30, TimeUnit.SECONDS);
    executor.shutdownNow();

    assertThat(successCount.get()).isEqualTo(5);

    BigDecimal finalBalance = ledgerEntryRepository.calculateBalance(from.getId());
    assertThat(finalBalance).isEqualByComparingTo("0.00");

    List<LedgerEntry> entries = ledgerEntryRepository.findAll();
    assertThat(entries).allMatch(entry -> entry.getAmount().compareTo(BigDecimal.ZERO) >= 0);
  }
}

