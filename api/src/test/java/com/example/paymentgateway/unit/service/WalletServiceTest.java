package com.example.paymentgateway.unit.service;

import com.example.paymentgateway.domain.entity.User;
import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.dto.BalanceResponse;
import com.example.paymentgateway.dto.WalletCreateRequest;
import com.example.paymentgateway.exception.NotFoundException;
import com.example.paymentgateway.repository.UserRepository;
import com.example.paymentgateway.repository.WalletRepository;
import com.example.paymentgateway.security.AccessControlService;
import com.example.paymentgateway.service.AuditService;
import com.example.paymentgateway.service.BalanceService;
import com.example.paymentgateway.service.IdempotencyService;
import com.example.paymentgateway.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class WalletServiceTest {

  @Test
  void shouldReturnBalanceFromBalanceService() {
    WalletRepository walletRepository = Mockito.mock(WalletRepository.class);
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    AccessControlService accessControlService = Mockito.mock(AccessControlService.class);
    IdempotencyService idempotencyService = Mockito.mock(IdempotencyService.class);
    BalanceService balanceService = Mockito.mock(BalanceService.class);
    AuditService auditService = Mockito.mock(AuditService.class);
    ObjectMapper objectMapper = new ObjectMapper();

    WalletService walletService = new WalletService(walletRepository, userRepository, accessControlService,
      idempotencyService, balanceService, auditService, objectMapper);

    UUID walletId = UUID.randomUUID();
    Wallet wallet = new Wallet();
    User user = new User();
    setField(wallet, "id", walletId);
    wallet.setUser(user);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(balanceService.getBalance(walletId)).thenReturn(new BigDecimal("123.45"));

    BalanceResponse response = walletService.getBalance(walletId);

    assertThat(response.getWalletId()).isEqualTo(walletId);
    assertThat(response.getBalance()).isEqualByComparingTo("123.45");
  }

  @Test
  void createWalletShouldThrowWhenUserMissing() {
    WalletRepository walletRepository = Mockito.mock(WalletRepository.class);
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    AccessControlService accessControlService = Mockito.mock(AccessControlService.class);
    IdempotencyService idempotencyService = Mockito.mock(IdempotencyService.class);
    BalanceService balanceService = Mockito.mock(BalanceService.class);
    AuditService auditService = Mockito.mock(AuditService.class);
    ObjectMapper objectMapper = new ObjectMapper();

    WalletService walletService = new WalletService(walletRepository, userRepository, accessControlService,
      idempotencyService, balanceService, auditService, objectMapper);

    WalletCreateRequest request = new WalletCreateRequest();
    request.setUserId(UUID.randomUUID());
    request.setCurrency("USD");

    when(userRepository.findById(request.getUserId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> walletService.createWallet(request))
      .isInstanceOf(NotFoundException.class);
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError("Failed to set field " + fieldName, ex);
    }
  }
}

