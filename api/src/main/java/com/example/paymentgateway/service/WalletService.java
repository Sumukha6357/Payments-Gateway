package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.entity.Wallet;
import com.example.paymentgateway.domain.enumtype.WalletStatus;
import com.example.paymentgateway.dto.BalanceResponse;
import com.example.paymentgateway.dto.WalletCreateRequest;
import com.example.paymentgateway.dto.WalletResponse;
import com.example.paymentgateway.exception.NotFoundException;
import com.example.paymentgateway.repository.UserRepository;
import com.example.paymentgateway.repository.WalletRepository;
import com.example.paymentgateway.security.AccessControlService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WalletService {
  private final WalletRepository walletRepository;
  private final UserRepository userRepository;
  private final AccessControlService accessControlService;
  private final IdempotencyService idempotencyService;
  private final BalanceService balanceService;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public WalletService(WalletRepository walletRepository, UserRepository userRepository, AccessControlService accessControlService,
                       IdempotencyService idempotencyService,
                       BalanceService balanceService,
                       AuditService auditService, ObjectMapper objectMapper) {
    this.walletRepository = walletRepository;
    this.userRepository = userRepository;
    this.accessControlService = accessControlService;
    this.idempotencyService = idempotencyService;
    this.balanceService = balanceService;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  public WalletResponse createWallet(WalletCreateRequest request) {
    accessControlService.assertCanAccessUser(request.getUserId());
    Wallet wallet = new Wallet();
    wallet.setUser(userRepository.findById(request.getUserId())
      .orElseThrow(() -> new NotFoundException("User not found")));
    wallet.setCurrency(request.getCurrency().toUpperCase());
    wallet.setStatus(WalletStatus.ACTIVE);
    Wallet saved = walletRepository.save(wallet);
    auditService.log("Wallet", saved.getId().toString(), "CREATE", toJson(saved));
    return toResponse(saved);
  }

  public IdempotencyResult<WalletResponse> createWalletIdempotent(WalletCreateRequest request, String key, String fingerprint) {
    return idempotencyService.execute(key, fingerprint, WalletResponse.class, () ->
      new IdempotencyResult<>(200, createWallet(request), false));
  }

  public WalletResponse getWallet(UUID id) {
    Wallet wallet = walletRepository.findById(id)
      .orElseThrow(() -> new NotFoundException("Wallet not found"));
    accessControlService.assertCanAccessUser(wallet.getUser().getId());
    return toResponse(wallet);
  }

  public BalanceResponse getBalance(UUID walletId) {
    Wallet wallet = walletRepository.findById(walletId)
      .orElseThrow(() -> new NotFoundException("Wallet not found"));
    accessControlService.assertCanAccessUser(wallet.getUser().getId());
    BalanceResponse response = new BalanceResponse();
    response.setWalletId(wallet.getId());
    response.setBalance(balanceService.getBalance(wallet.getId()));
    return response;
  }

  public Page<WalletResponse> listWallets(Pageable pageable) {
    return walletRepository.findAll(pageable).map(this::toResponse);
  }

  private WalletResponse toResponse(Wallet wallet) {
    WalletResponse response = new WalletResponse();
    response.setId(wallet.getId());
    response.setUserId(wallet.getUser().getId());
    response.setCurrency(wallet.getCurrency());
    response.setStatus(wallet.getStatus().name());
    response.setCreatedAt(wallet.getCreatedAt());
    return response;
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }
}
