package com.example.paymentgateway.controller;

import com.example.paymentgateway.dto.BalanceResponse;
import com.example.paymentgateway.dto.WalletCreateRequest;
import com.example.paymentgateway.dto.WalletResponse;
import com.example.paymentgateway.service.IdempotencyResult;
import com.example.paymentgateway.service.IdempotencyService;
import com.example.paymentgateway.service.WalletService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class WalletController {
  private final WalletService walletService;
  private final IdempotencyService idempotencyService;
  private final ObjectMapper objectMapper;

  public WalletController(WalletService walletService, IdempotencyService idempotencyService, ObjectMapper objectMapper) {
    this.walletService = walletService;
    this.idempotencyService = idempotencyService;
    this.objectMapper = objectMapper;
  }

  @Operation(summary = "Create wallet")
  @PostMapping
  public ResponseEntity<WalletResponse> create(@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                               @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                 required = true,
                                                 content = @Content(schema = @Schema(implementation = WalletCreateRequest.class),
                                                   examples = @ExampleObject(
                                                     name = "CreateWallet",
                                                     value = "{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"currency\":\"USD\"}"
                                                   ))
                                               )
                                               @Valid @RequestBody WalletCreateRequest request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return ResponseEntity.ok(walletService.createWallet(request));
    }
    String fingerprint = idempotencyService.fingerprint("POST", "/wallets", toJson(request) + "|" + idempotencyKey);
    IdempotencyResult<WalletResponse> result = walletService.createWalletIdempotent(request, idempotencyKey, fingerprint);
    return ResponseEntity.status(result.getStatus()).body(result.getBody());
  }

  @GetMapping("/{id}")
  public WalletResponse get(@PathVariable UUID id) {
    return walletService.getWallet(id);
  }

  @GetMapping("/{id}/balance")
  public BalanceResponse getBalance(@PathVariable UUID id) {
    return walletService.getBalance(id);
  }

  @GetMapping
  public List<WalletResponse> list(Pageable pageable) {
    return walletService.listWallets(pageable).getContent();
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }
}
