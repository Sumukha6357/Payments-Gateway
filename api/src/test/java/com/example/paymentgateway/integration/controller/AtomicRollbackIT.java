package com.example.paymentgateway.integration.controller;

import com.example.paymentgateway.domain.entity.LedgerEntry;
import com.example.paymentgateway.domain.entity.User;
import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.domain.enumtype.LedgerEntryType;
import com.example.paymentgateway.dto.TransferRequest;
import com.example.paymentgateway.integration.support.IntegrationTestBase;
import com.example.paymentgateway.repository.LedgerEntryRepository;
import com.example.paymentgateway.repository.TransactionRepository;
import com.example.paymentgateway.repository.UserRepository;
import com.example.paymentgateway.repository.WalletRepository;
import com.example.paymentgateway.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AtomicRollbackIT extends IntegrationTestBase {

  @Autowired
  private TransactionService transactionService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WalletRepository walletRepository;

  @Autowired
  private TransactionRepository transactionRepository;

  @SpyBean
  private LedgerEntryRepository ledgerEntryRepository;

  @Test
  void shouldRollbackOnMidwayFailure() {
    User user = new User();
    user.setEmail("rollback1@example.com");
    user.setName("Rollback1");
    userRepository.save(user);

    User user2 = new User();
    user2.setEmail("rollback2@example.com");
    user2.setName("Rollback2");
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

    AtomicInteger saveCount = new AtomicInteger();
    Mockito.doAnswer(invocation -> {
      if (saveCount.incrementAndGet() == 2) {
        throw new RuntimeException("boom");
      }
      return invocation.callRealMethod();
    }).when(ledgerEntryRepository).save(Mockito.any(LedgerEntry.class));

    long ledgerBefore = ledgerEntryRepository.count();
    long txnBefore = transactionRepository.count();

    TransferRequest request = new TransferRequest();
    request.setFromWalletId(from.getId());
    request.setToWalletId(to.getId());
    request.setAmount(new BigDecimal("10.00"));

    assertThatThrownBy(() -> transactionService.transfer(request, "idem-rollback", "fp-rollback"))
      .isInstanceOf(RuntimeException.class);

    assertThat(ledgerEntryRepository.count()).isEqualTo(ledgerBefore);
    assertThat(transactionRepository.count()).isEqualTo(txnBefore);
  }
}

