package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.entity.Payment;
import com.example.paymentgateway.domain.entity.Transaction;
import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.domain.enumtype.PaymentStatus;
import com.example.paymentgateway.domain.enumtype.TransactionStatus;
import com.example.paymentgateway.domain.enumtype.WalletStatus;
import com.example.paymentgateway.dto.PaymentRequest;
import com.example.paymentgateway.dto.PaymentResponse;
import com.example.paymentgateway.dto.PaymentWebhookRequest;
import com.example.paymentgateway.exception.BadRequestException;
import com.example.paymentgateway.exception.NotFoundException;
import com.example.paymentgateway.repository.PaymentRepository;
import com.example.paymentgateway.repository.TransactionRepository;
import com.example.paymentgateway.repository.WalletRepository;
import com.example.paymentgateway.security.AccessControlService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
public class PaymentService {
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final PaymentRepository paymentRepository;
  private final LedgerService ledgerService;
  private final IdempotencyService idempotencyService;
  private final AccessControlService accessControlService;
  private final BalanceService balanceService;
  private final AuditService auditService;
  private final OutboxService outboxService;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;

  public PaymentService(WalletRepository walletRepository,
                        TransactionRepository transactionRepository,
                        PaymentRepository paymentRepository,
                        LedgerService ledgerService,
                        IdempotencyService idempotencyService,
                        AccessControlService accessControlService,
                        BalanceService balanceService,
                        AuditService auditService,
                        OutboxService outboxService,
                        ObjectMapper objectMapper,
                        MeterRegistry meterRegistry) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.paymentRepository = paymentRepository;
    this.ledgerService = ledgerService;
    this.idempotencyService = idempotencyService;
    this.accessControlService = accessControlService;
    this.balanceService = balanceService;
    this.auditService = auditService;
    this.outboxService = outboxService;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
  }

  @Transactional
  public IdempotencyResult<PaymentResponse> createPayment(PaymentRequest request, String idempotencyKey, String requestFingerprint) {
    return idempotencyService.execute(idempotencyKey, requestFingerprint, PaymentResponse.class, () -> {
      Wallet wallet = walletRepository.findByIdForUpdate(request.getWalletId())
        .orElseThrow(() -> new NotFoundException("Wallet not found"));
      accessControlService.assertCanAccessUser(wallet.getUser().getId());
      if (wallet.getStatus() != WalletStatus.ACTIVE) {
        throw new BadRequestException("Wallet not active");
      }

      Transaction txn = new Transaction();
      txn.setToWallet(wallet);
      txn.setAmount(request.getAmount());
      txn.setStatus(TransactionStatus.PENDING);
      txn.setIdempotencyKey(idempotencyKey);
      transactionRepository.save(txn);

      Payment payment = new Payment();
      payment.setTransaction(txn);
      payment.setExternalReference("PAY-" + UUID.randomUUID());
      payment.setStatus(PaymentStatus.PENDING);
      paymentRepository.save(payment);

      auditService.log("Payment", payment.getId().toString(), "CREATE", toJson(payment));
      outboxService.enqueue("Payment", payment.getId().toString(), "PAYMENT_CREATED", payment);

      PaymentResponse response = new PaymentResponse();
      response.setPaymentId(payment.getId());
      response.setTransactionId(txn.getId());
      response.setExternalReference(payment.getExternalReference());
      response.setAmount(txn.getAmount());
      response.setStatus(payment.getStatus().name());
      response.setCreatedAt(payment.getCreatedAt());
      return new IdempotencyResult<>(200, response, false);
    });
  }

  @Transactional
  public void handleWebhook(PaymentWebhookRequest request) {
    Payment payment = paymentRepository.findByExternalReference(request.getExternalReference())
      .orElseThrow(() -> new NotFoundException("Payment not found"));

    if (payment.getStatus() == PaymentStatus.SUCCESS) {
      return;
    }

    Transaction txn = payment.getTransaction();
    Wallet wallet = walletRepository.findByIdForUpdate(txn.getToWallet().getId())
      .orElseThrow(() -> new NotFoundException("Wallet not found"));
    if (request.getAmount().compareTo(txn.getAmount()) != 0) {
      throw new BadRequestException("Amount mismatch");
    }

    if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
      ledgerService.postCredit(txn, wallet, request.getAmount());

      txn.setStatus(TransactionStatus.SUCCESS);
      payment.setStatus(PaymentStatus.SUCCESS);

      auditService.log("Payment", payment.getId().toString(), "WEBHOOK_SUCCESS", toJson(request));
      outboxService.enqueue("Payment", payment.getId().toString(), "PAYMENT_SUCCESS", request);
      registerBalanceEviction(wallet.getId());
      meterRegistry.counter("payment_success_total").increment();
    } else {
      txn.setStatus(TransactionStatus.FAILED);
      payment.setStatus(PaymentStatus.FAILED);
      auditService.log("Payment", payment.getId().toString(), "WEBHOOK_FAILED", toJson(request));
      outboxService.enqueue("Payment", payment.getId().toString(), "PAYMENT_FAILED", request);
      meterRegistry.counter("payment_failure_total").increment();
    }
  }

  private void registerBalanceEviction(UUID walletId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          balanceService.evictBalance(walletId);
        }
      });
    } else {
      balanceService.evictBalance(walletId);
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  public Page<PaymentResponse> listPayments(Pageable pageable) {
    return paymentRepository.findAll(pageable).map(this::toResponse);
  }

  public PaymentResponse getPayment(java.util.UUID paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
      .orElseThrow(() -> new NotFoundException("Payment not found"));
    return toResponse(payment);
  }

  private PaymentResponse toResponse(Payment payment) {
    PaymentResponse response = new PaymentResponse();
    response.setPaymentId(payment.getId());
    response.setTransactionId(payment.getTransaction().getId());
    response.setExternalReference(payment.getExternalReference());
    response.setAmount(payment.getTransaction().getAmount());
    response.setStatus(payment.getStatus().name());
    response.setCreatedAt(payment.getCreatedAt());
    return response;
  }
}
