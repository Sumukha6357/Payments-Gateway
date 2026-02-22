package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.entity.Transaction;
import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.domain.enumtype.TransactionStatus;
import com.example.paymentgateway.domain.enumtype.WalletStatus;
import com.example.paymentgateway.dto.TransferRequest;
import com.example.paymentgateway.dto.TransferResponse;
import com.example.paymentgateway.exception.BadRequestException;
import com.example.paymentgateway.exception.NotFoundException;
import com.example.paymentgateway.repository.TransactionRepository;
import com.example.paymentgateway.repository.WalletRepository;
import com.example.paymentgateway.security.AccessControlService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.OptimisticLockException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;

@Service
public class TransactionService {
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final IdempotencyService idempotencyService;
  private final LedgerService ledgerService;
  private final AccessControlService accessControlService;
  private final BalanceService balanceService;
  private final AuditService auditService;
  private final OutboxService outboxService;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;

  public TransactionService(WalletRepository walletRepository,
                            TransactionRepository transactionRepository,
                            IdempotencyService idempotencyService,
                            LedgerService ledgerService,
                            AccessControlService accessControlService,
                            BalanceService balanceService,
                            AuditService auditService,
                            OutboxService outboxService,
                            ObjectMapper objectMapper,
                            MeterRegistry meterRegistry) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.idempotencyService = idempotencyService;
    this.ledgerService = ledgerService;
    this.accessControlService = accessControlService;
    this.balanceService = balanceService;
    this.auditService = auditService;
    this.outboxService = outboxService;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
  }

  public IdempotencyResult<TransferResponse> transfer(TransferRequest request, String idempotencyKey, String requestFingerprint) {
    if (request.getFromWalletId().equals(request.getToWalletId())) {
      throw new BadRequestException("fromWalletId and toWalletId must differ");
    }

    int attempts = 0;
    while (true) {
      attempts++;
      try {
        IdempotencyResult<TransferResponse> result = transferInternal(request, idempotencyKey, requestFingerprint);
        meterRegistry.counter("transfer_success_total").increment();
        return result;
      } catch (OptimisticLockException | ObjectOptimisticLockingFailureException ex) {
        meterRegistry.counter("wallet_lock_contention").increment();
        if (attempts >= 3) {
          meterRegistry.counter("transfer_failure_total").increment();
          throw ex;
        }
      } catch (RuntimeException ex) {
        meterRegistry.counter("transfer_failure_total").increment();
        throw ex;
      }
    }
  }

  @Transactional
  private IdempotencyResult<TransferResponse> transferInternal(TransferRequest request, String idempotencyKey, String requestFingerprint) {
    return idempotencyService.execute(idempotencyKey, requestFingerprint, TransferResponse.class, () -> {
      Wallet fromWallet = walletRepository.findByIdForUpdate(request.getFromWalletId())
        .orElseThrow(() -> new NotFoundException("From wallet not found"));
      Wallet toWallet = walletRepository.findByIdForUpdate(request.getToWalletId())
        .orElseThrow(() -> new NotFoundException("To wallet not found"));
      accessControlService.assertCanAccessUser(fromWallet.getUser().getId());

      if (fromWallet.getStatus() != WalletStatus.ACTIVE || toWallet.getStatus() != WalletStatus.ACTIVE) {
        throw new BadRequestException("Wallet is not active");
      }
      if (!fromWallet.getCurrency().equalsIgnoreCase(toWallet.getCurrency())) {
        throw new BadRequestException("Currency mismatch");
      }

      Transaction txn = new Transaction();
      txn.setFromWallet(fromWallet);
      txn.setToWallet(toWallet);
      txn.setAmount(request.getAmount());
      txn.setStatus(TransactionStatus.PENDING);
      txn.setIdempotencyKey(idempotencyKey);
      transactionRepository.save(txn);

      ledgerService.postTransfer(txn, fromWallet, toWallet, request.getAmount());

      txn.setStatus(TransactionStatus.SUCCESS);

      auditService.log("Transaction", txn.getId().toString(), "TRANSFER", toJson(txn));
      outboxService.enqueue("Transaction", txn.getId().toString(), "TRANSFER_SUCCESS", txn);

      registerBalanceEviction(fromWallet.getId(), toWallet.getId());

      TransferResponse response = new TransferResponse();
      response.setTransactionId(txn.getId());
      response.setFromWalletId(fromWallet.getId());
      response.setToWalletId(toWallet.getId());
      response.setAmount(txn.getAmount());
      response.setStatus(txn.getStatus().name());
      response.setCreatedAt(txn.getCreatedAt());
      return new IdempotencyResult<>(200, response, false);
    });
  }

  private void registerBalanceEviction(UUID fromWalletId, UUID toWalletId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          balanceService.evictBalance(fromWalletId);
          balanceService.evictBalance(toWalletId);
        }
      });
    } else {
      balanceService.evictBalance(fromWalletId);
      balanceService.evictBalance(toWalletId);
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  public Page<TransferResponse> listTransfers(Pageable pageable) {
    return transactionRepository.findAll(pageable).map(txn -> {
      TransferResponse response = new TransferResponse();
      response.setTransactionId(txn.getId());
      response.setFromWalletId(txn.getFromWallet() != null ? txn.getFromWallet().getId() : null);
      response.setToWalletId(txn.getToWallet() != null ? txn.getToWallet().getId() : null);
      response.setAmount(txn.getAmount());
      response.setStatus(txn.getStatus().name());
      response.setCreatedAt(txn.getCreatedAt());
      return response;
    });
  }
}
