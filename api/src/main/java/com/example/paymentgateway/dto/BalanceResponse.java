package com.example.paymentgateway.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class BalanceResponse {
  private UUID walletId;
  private BigDecimal balance;

  public UUID getWalletId() {
    return walletId;
  }

  public void setWalletId(UUID walletId) {
    this.walletId = walletId;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }
}
