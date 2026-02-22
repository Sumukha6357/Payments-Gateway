package com.example.paymentgateway.service;

import com.example.paymentgateway.repository.LedgerEntryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class BalanceService {
  private final LedgerEntryRepository ledgerEntryRepository;

  public BalanceService(LedgerEntryRepository ledgerEntryRepository) {
    this.ledgerEntryRepository = ledgerEntryRepository;
  }

  @Cacheable(value = "walletBalance", key = "#p0")
  public BigDecimal getBalance(UUID walletId) {
    return ledgerEntryRepository.calculateBalance(walletId);
  }

  @CacheEvict(value = "walletBalance", key = "#p0")
  public void evictBalance(UUID walletId) {
  }
}
