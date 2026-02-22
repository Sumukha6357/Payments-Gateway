package com.example.paymentgateway.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TransferResponse {
  private UUID transactionId;
  private UUID fromWalletId;
  private UUID toWalletId;
  private BigDecimal amount;
  private String status;
  private OffsetDateTime createdAt;

  public UUID getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(UUID transactionId) {
    this.transactionId = transactionId;
  }

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
