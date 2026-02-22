package com.example.paymentgateway.controller;

import com.example.paymentgateway.dto.TransferRequest;
import com.example.paymentgateway.dto.TransferResponse;
import com.example.paymentgateway.exception.BadRequestException;
import com.example.paymentgateway.service.IdempotencyResult;
import com.example.paymentgateway.service.IdempotencyService;
import com.example.paymentgateway.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class TransactionController {
  private final TransactionService transactionService;
  private final IdempotencyService idempotencyService;
  private final ObjectMapper objectMapper;

  public TransactionController(TransactionService transactionService,
                               IdempotencyService idempotencyService,
                               ObjectMapper objectMapper) {
    this.transactionService = transactionService;
    this.idempotencyService = idempotencyService;
    this.objectMapper = objectMapper;
  }

  @Operation(summary = "Transfer funds between wallets")
  @PostMapping("/transfer")
  public ResponseEntity<TransferResponse> transfer(
    @Parameter(description = "Idempotency key", required = true)
    @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = @Content(schema = @Schema(implementation = TransferRequest.class), examples = @ExampleObject(
        name = "CreateTransfer",
        value = "{\"fromWalletId\":\"00000000-0000-0000-0000-000000000001\",\"toWalletId\":\"00000000-0000-0000-0000-000000000002\",\"amount\":10.00}"
      ))
    )
    @Valid @RequestBody TransferRequest request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new BadRequestException("Idempotency-Key header required");
    }
    String fingerprint = idempotencyService.fingerprint("POST", "/transactions/transfer", toJson(request) + "|" + idempotencyKey);
    IdempotencyResult<TransferResponse> result = transactionService.transfer(request, idempotencyKey, fingerprint);
    return ResponseEntity.status(result.getStatus()).body(result.getBody());
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  @GetMapping
  public List<TransferResponse> list(Pageable pageable) {
    return transactionService.listTransfers(pageable).getContent();
  }
}
