package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.entity.LedgerEntry;
import com.example.paymentgateway.domain.entity.Transaction;
import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.domain.enumtype.LedgerEntryType;
import com.example.paymentgateway.exception.InsufficientFundsException;
import com.example.paymentgateway.exception.LedgerInvariantViolationException;
import com.example.paymentgateway.repository.LedgerEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {
  private final LedgerEntryRepository ledgerEntryRepository;
  private final boolean noOverdraft;

  public LedgerService(LedgerEntryRepository ledgerEntryRepository,
                       @Value("${ledger.no-overdraft:true}") boolean noOverdraft) {
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.noOverdraft = noOverdraft;
  }

  public void postTransfer(Transaction transaction, Wallet fromWallet, Wallet toWallet, BigDecimal amount) {
    validateAmount(amount);
    validateNoOverdraft(fromWallet.getId(), amount);
    LedgerEntry debit = buildEntry(fromWallet, LedgerEntryType.DEBIT, amount, transaction.getId());
    LedgerEntry credit = buildEntry(toWallet, LedgerEntryType.CREDIT, amount, transaction.getId());
    ledgerEntryRepository.save(debit);
    ledgerEntryRepository.save(credit);
    assertBalanced(transaction.getId());
  }

  public void postCredit(Transaction transaction, Wallet wallet, BigDecimal amount) {
    validateAmount(amount);
    LedgerEntry credit = buildEntry(wallet, LedgerEntryType.CREDIT, amount, transaction.getId());
    ledgerEntryRepository.save(credit);
  }

  public void assertBalanced(UUID referenceId) {
    List<LedgerEntry> entries = ledgerEntryRepository.findByReferenceId(referenceId);
    if (entries.isEmpty()) {
      throw new LedgerInvariantViolationException("No ledger entries found for reference " + referenceId);
    }
    BigDecimal net = entries.stream()
      .map(e -> e.getType() == LedgerEntryType.CREDIT ? e.getAmount() : e.getAmount().negate())
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (net.compareTo(BigDecimal.ZERO) != 0) {
      throw new LedgerInvariantViolationException("Ledger entries are not balanced for reference " + referenceId);
    }
  }

  private LedgerEntry buildEntry(Wallet wallet, LedgerEntryType type, BigDecimal amount, UUID referenceId) {
    LedgerEntry entry = new LedgerEntry();
    entry.setWallet(wallet);
    entry.setType(type);
    entry.setAmount(amount);
    entry.setReferenceId(referenceId);
    return entry;
  }

  private void validateNoOverdraft(UUID walletId, BigDecimal debitAmount) {
    if (!noOverdraft) {
      return;
    }
    BigDecimal balance = ledgerEntryRepository.calculateBalance(walletId);
    if (balance.compareTo(debitAmount) < 0) {
      throw new InsufficientFundsException("Insufficient balance");
    }
  }

  private void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new LedgerInvariantViolationException("Amount must be positive");
    }
  }
}
