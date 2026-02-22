package com.example.paymentgateway.unit.service;

import com.example.paymentgateway.domain.entity.Transaction;
import com.example.paymentgateway.domain.entity.User;
import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.domain.enumtype.TransactionStatus;
import com.example.paymentgateway.domain.enumtype.WalletStatus;
import com.example.paymentgateway.dto.TransferRequest;
import com.example.paymentgateway.dto.TransferResponse;
import com.example.paymentgateway.repository.TransactionRepository;
import com.example.paymentgateway.repository.WalletRepository;
import com.example.paymentgateway.security.AccessControlService;
import com.example.paymentgateway.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TransactionServiceTest {

  @Test
  void shouldReturnIdempotentResult() {
    WalletRepository walletRepository = Mockito.mock(WalletRepository.class);
    TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);
    IdempotencyService idempotencyService = Mockito.mock(IdempotencyService.class);
    LedgerService ledgerService = Mockito.mock(LedgerService.class);
    AccessControlService accessControlService = Mockito.mock(AccessControlService.class);
    BalanceService balanceService = Mockito.mock(BalanceService.class);
    AuditService auditService = Mockito.mock(AuditService.class);
    OutboxService outboxService = Mockito.mock(OutboxService.class);

    TransactionService service = new TransactionService(walletRepository, transactionRepository, idempotencyService,
      ledgerService, accessControlService, balanceService, auditService, outboxService, new ObjectMapper(),
      new SimpleMeterRegistry());

    TransferResponse fixed = new TransferResponse();
    fixed.setTransactionId(UUID.randomUUID());
    fixed.setStatus(TransactionStatus.SUCCESS.name());
    when(idempotencyService.execute(anyString(), anyString(), eq(TransferResponse.class), any()))
      .thenReturn(new IdempotencyResult<>(200, fixed, true));

    TransferRequest request = new TransferRequest();
    request.setFromWalletId(UUID.randomUUID());
    request.setToWalletId(UUID.randomUUID());
    request.setAmount(new BigDecimal("10.00"));

    IdempotencyResult<TransferResponse> result = service.transfer(request, "idem-1", "fp");
    assertThat(result.getBody().getTransactionId()).isEqualTo(fixed.getTransactionId());
  }

  @Test
  void shouldRetryOnOptimisticLockAndEventuallySucceed() {
    WalletRepository walletRepository = Mockito.mock(WalletRepository.class);
    TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);
    IdempotencyService idempotencyService = Mockito.mock(IdempotencyService.class);
    LedgerService ledgerService = Mockito.mock(LedgerService.class);
    AccessControlService accessControlService = Mockito.mock(AccessControlService.class);
    BalanceService balanceService = Mockito.mock(BalanceService.class);
    AuditService auditService = Mockito.mock(AuditService.class);
    OutboxService outboxService = Mockito.mock(OutboxService.class);

    TransactionService service = new TransactionService(walletRepository, transactionRepository, idempotencyService,
      ledgerService, accessControlService, balanceService, auditService, outboxService, new ObjectMapper(),
      new SimpleMeterRegistry());

    TransferRequest request = new TransferRequest();
    request.setFromWalletId(UUID.randomUUID());
    request.setToWalletId(UUID.randomUUID());
    request.setAmount(new BigDecimal("10.00"));

    when(idempotencyService.execute(anyString(), anyString(), eq(TransferResponse.class), any())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      var supplier = (java.util.function.Supplier<IdempotencyResult<TransferResponse>>) invocation.getArgument(3);
      return supplier.get();
    });

    Wallet from = new Wallet();
    Wallet to = new Wallet();
    User fromUser = new User();
    User toUser = new User();
    setField(fromUser, "id", UUID.randomUUID());
    setField(toUser, "id", UUID.randomUUID());
    from.setUser(fromUser);
    to.setUser(toUser);
    from.setCurrency("USD");
    to.setCurrency("USD");
    from.setStatus(WalletStatus.ACTIVE);
    to.setStatus(WalletStatus.ACTIVE);
    setField(from, "id", request.getFromWalletId());
    setField(to, "id", request.getToWalletId());

    when(walletRepository.findByIdForUpdate(request.getFromWalletId()))
      .thenThrow(new ObjectOptimisticLockingFailureException(Wallet.class, UUID.randomUUID()))
      .thenReturn(Optional.of(from));
    when(walletRepository.findByIdForUpdate(request.getToWalletId())).thenReturn(Optional.of(to));
    when(transactionRepository.save(any())).thenAnswer(invocation -> {
      Transaction txn = invocation.getArgument(0);
      if (txn.getId() == null) {
        setField(txn, "id", UUID.randomUUID());
      }
      return txn;
    });

    IdempotencyResult<TransferResponse> response = service.transfer(request, "idem-2", "fp");
    assertThat(response.getBody().getStatus()).isEqualTo(TransactionStatus.SUCCESS.name());
    verify(ledgerService).postTransfer(any(Transaction.class), eq(from), eq(to), eq(new BigDecimal("10.00")));
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
