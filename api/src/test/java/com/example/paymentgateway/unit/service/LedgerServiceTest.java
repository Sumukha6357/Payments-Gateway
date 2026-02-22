package com.example.paymentgateway.unit.service;

import com.example.paymentgateway.domain.entity.LedgerEntry;
import com.example.paymentgateway.domain.entity.Transaction;
import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.domain.enumtype.LedgerEntryType;
import com.example.paymentgateway.exception.InsufficientFundsException;
import com.example.paymentgateway.exception.LedgerInvariantViolationException;
import com.example.paymentgateway.repository.LedgerEntryRepository;
import com.example.paymentgateway.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class LedgerServiceTest {
  @Test
  void shouldRejectOverdraftWhenPolicyEnabled() {
    LedgerEntryRepository repository = Mockito.mock(LedgerEntryRepository.class);
    LedgerService service = new LedgerService(repository, true);
    Transaction txn = new Transaction();
    Wallet from = wallet(UUID.randomUUID());
    Wallet to = wallet(UUID.randomUUID());
    setField(txn, "id", UUID.randomUUID());
    when(repository.calculateBalance(from.getId())).thenReturn(new BigDecimal("5.00"));

    assertThatThrownBy(() -> service.postTransfer(txn, from, to, new BigDecimal("10.00")))
      .isInstanceOf(InsufficientFundsException.class);
  }

  @Test
  void shouldAssertBalancedEntries() {
    LedgerEntryRepository repository = Mockito.mock(LedgerEntryRepository.class);
    LedgerService service = new LedgerService(repository, true);
    UUID referenceId = UUID.randomUUID();

    LedgerEntry debit = entry(LedgerEntryType.DEBIT, new BigDecimal("10.00"), referenceId);
    LedgerEntry credit = entry(LedgerEntryType.CREDIT, new BigDecimal("5.00"), referenceId);
    when(repository.findByReferenceId(referenceId)).thenReturn(List.of(debit, credit));

    assertThatThrownBy(() -> service.assertBalanced(referenceId))
      .isInstanceOf(LedgerInvariantViolationException.class);
  }

  @Test
  void randomizedBalancedOperationsShouldPassInvariant() {
    LedgerEntryRepository repository = Mockito.mock(LedgerEntryRepository.class);
    LedgerService service = new LedgerService(repository, false);
    Random random = new Random(42);

    for (int i = 0; i < 100; i++) {
      UUID referenceId = UUID.randomUUID();
      BigDecimal amount = BigDecimal.valueOf(random.nextInt(5000) + 1, 2);
      List<LedgerEntry> entries = new ArrayList<>();
      entries.add(entry(LedgerEntryType.DEBIT, amount, referenceId));
      entries.add(entry(LedgerEntryType.CREDIT, amount, referenceId));
      when(repository.findByReferenceId(referenceId)).thenReturn(entries);

      assertThatCode(() -> service.assertBalanced(referenceId)).doesNotThrowAnyException();
    }
  }

  private static LedgerEntry entry(LedgerEntryType type, BigDecimal amount, UUID referenceId) {
    LedgerEntry entry = new LedgerEntry();
    entry.setType(type);
    entry.setAmount(amount);
    entry.setReferenceId(referenceId);
    Wallet wallet = wallet(UUID.randomUUID());
    entry.setWallet(wallet);
    return entry;
  }

  private static Wallet wallet(UUID id) {
    Wallet wallet = new Wallet();
    setField(wallet, "id", id);
    return wallet;
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
  }
}
