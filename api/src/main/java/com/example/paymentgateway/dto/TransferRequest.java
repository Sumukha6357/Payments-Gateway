package com.example.paymentgateway.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class TransferRequest {
  @NotNull
  private UUID fromWalletId;

  @NotNull
  private UUID toWalletId;

  @NotNull
  @DecimalMin("0.01")
  private BigDecimal amount;

  public UUID getFromWalletId() {
    return fromWalletId;
  }

  public void setFromWalletId(UUID fromWalletId) {
    this.fromWalletId = fromWalletId;
  }

  public UUID getToWalletId() {
    return toWalletId;
  }

  public void setToWalletId(UUID toWalletId) {
    this.toWalletId = toWalletId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
