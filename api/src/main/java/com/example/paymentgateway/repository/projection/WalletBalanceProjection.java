package com.example.paymentgateway.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletBalanceProjection {
  UUID getWalletId();

  BigDecimal getBalance();
}
