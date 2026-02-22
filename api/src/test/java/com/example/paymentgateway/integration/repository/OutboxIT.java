package com.example.paymentgateway.integration.repository;

import com.example.paymentgateway.domain.entity.LedgerEntry;
import com.example.paymentgateway.domain.entity.OutboxEvent;
import com.example.paymentgateway.domain.entity.User;
import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.domain.enumtype.LedgerEntryType;
import com.example.paymentgateway.domain.enumtype.OutboxStatus;
import com.example.paymentgateway.dto.TransferRequest;
import com.example.paymentgateway.integration.support.IntegrationTestBase;
import com.example.paymentgateway.repository.LedgerEntryRepository;
import com.example.paymentgateway.repository.OutboxEventRepository;
import com.example.paymentgateway.repository.UserRepository;
import com.example.paymentgateway.repository.WalletRepository;
import com.example.paymentgateway.service.OutboxService;
import com.example.paymentgateway.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxIT extends IntegrationTestBase {

  @Autowired
  private TransactionService transactionService;

  @Autowired
  private OutboxService outboxService;

  @Autowired
  private OutboxEventRepository outboxEventRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WalletRepository walletRepository;

  @Autowired
  private LedgerEntryRepository ledgerEntryRepository;

  @Test
  @WithMockUser(username = "outbox-user", roles = "USER")
  void shouldDispatchOutboxEvents() {
    User user = new User();
    user.setEmail("outbox1@example.com");
    user.setName("Outbox1");
    userRepository.save(user);

    User user2 = new User();
    user2.setEmail("outbox2@example.com");
    user2.setName("Outbox2");
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
    credit.setAmount(new BigDecimal("50.00"));
    credit.setReferenceId(UUID.randomUUID());
    ledgerEntryRepository.save(credit);

    TransferRequest request = new TransferRequest();
    request.setFromWalletId(from.getId());
    request.setToWalletId(to.getId());
    request.setAmount(new BigDecimal("10.00"));

    transactionService.transfer(request, "idem-outbox", "fp-outbox");

    List<OutboxEvent> pending = outboxEventRepository.findDispatchable(List.of(OutboxStatus.PENDING), OffsetDateTime.now().plusSeconds(1));
    assertThat(pending).isNotEmpty();

    outboxService.dispatchPending();

    List<OutboxEvent> sent = outboxEventRepository.findDispatchable(List.of(OutboxStatus.SENT), OffsetDateTime.now().plusSeconds(1));
    assertThat(sent).isNotEmpty();
  }
}

