package com.example.paymentgateway.controller;

import com.example.paymentgateway.dto.PaymentRequest;
import com.example.paymentgateway.dto.PaymentResponse;
import com.example.paymentgateway.dto.PaymentWebhookRequest;
import com.example.paymentgateway.exception.SignatureInvalidException;
import com.example.paymentgateway.service.IdempotencyResult;
import com.example.paymentgateway.service.IdempotencyService;
import com.example.paymentgateway.service.PaymentService;
import com.example.paymentgateway.service.WebhookReplayProtectionService;
import com.example.paymentgateway.util.WebhookSignatureVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {
  private final PaymentService paymentService;
  private final IdempotencyService idempotencyService;
  private final WebhookSignatureVerifier signatureVerifier;
  private final WebhookReplayProtectionService replayProtectionService;
  private final ObjectMapper objectMapper;

  public PaymentController(PaymentService paymentService,
                           IdempotencyService idempotencyService,
                           WebhookSignatureVerifier signatureVerifier,
                           WebhookReplayProtectionService replayProtectionService,
                           ObjectMapper objectMapper) {
    this.paymentService = paymentService;
    this.idempotencyService = idempotencyService;
    this.signatureVerifier = signatureVerifier;
    this.replayProtectionService = replayProtectionService;
    this.objectMapper = objectMapper;
  }

  @Operation(
    summary = "Create payment",
    security = @SecurityRequirement(name = "bearerAuth"),
    responses = {
      @ApiResponse(responseCode = "200", description = "Payment created"),
      @ApiResponse(responseCode = "409", description = "Idempotency key conflict")
    }
  )
  @PostMapping
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<PaymentResponse> create(
    @Parameter(description = "Idempotency key", required = true)
    @RequestHeader("Idempotency-Key") String idempotencyKey,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = @Content(schema = @Schema(implementation = PaymentRequest.class), examples = @ExampleObject(
        name = "CreatePayment",
        value = "{\"walletId\":\"00000000-0000-0000-0000-000000000001\",\"amount\":25.00}"
      ))
    )
    @Valid @RequestBody PaymentRequest request) {
    String fingerprint = idempotencyService.fingerprint("POST", "/payments", toJson(request) + "|" + idempotencyKey);
    IdempotencyResult<PaymentResponse> result = paymentService.createPayment(request, idempotencyKey, fingerprint);
    return ResponseEntity.status(result.getStatus()).body(result.getBody());
  }

  @Operation(summary = "Receive payment webhook")
  @PostMapping("/webhook")
  public void webhook(@RequestHeader(name = "X-Signature", required = false) String signature,
                      @RequestHeader(name = "X-Timestamp", required = false) String timestamp,
                      @RequestHeader(name = "X-Event-Id", required = false) String eventId,
                      @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        required = true,
                        content = @Content(schema = @Schema(implementation = PaymentWebhookRequest.class), examples = @ExampleObject(
                          name = "WebhookEvent",
                          value = "{\"externalReference\":\"PAY-123\",\"status\":\"SUCCESS\",\"amount\":25.00}"
                        ))
                      )
                      @Valid @RequestBody PaymentWebhookRequest request) {
    replayProtectionService.assertFresh(eventId, timestamp);
    if (!signatureVerifier.isValid(signature, request, timestamp)) {
      throw new SignatureInvalidException("Invalid webhook signature");
    }
    paymentService.handleWebhook(request);
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<PaymentResponse> list(Pageable pageable) {
    return paymentService.listPayments(pageable).getContent();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public PaymentResponse get(@PathVariable UUID id) {
    return paymentService.getPayment(id);
  }
}
