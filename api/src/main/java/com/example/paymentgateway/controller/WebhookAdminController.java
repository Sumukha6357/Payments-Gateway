package com.example.paymentgateway.controller;

import com.example.paymentgateway.dto.WebhookEndpointRequest;
import com.example.paymentgateway.dto.WebhookEndpointResponse;
import com.example.paymentgateway.service.WebhookEndpointService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/admin/webhooks")
@PreAuthorize("hasRole('ADMIN')")
public class WebhookAdminController {
  private final WebhookEndpointService webhookEndpointService;

  public WebhookAdminController(WebhookEndpointService webhookEndpointService) {
    this.webhookEndpointService = webhookEndpointService;
  }

  @Operation(summary = "Register webhook endpoint")
  @PostMapping
  public WebhookEndpointResponse create(@Valid @RequestBody WebhookEndpointRequest request) {
    return webhookEndpointService.create(request);
  }

  @Operation(summary = "List webhook endpoints")
  @GetMapping
  public List<WebhookEndpointResponse> list() {
    return webhookEndpointService.list();
  }

  @Operation(summary = "Disable webhook endpoint")
  @DeleteMapping("/{id}")
  public WebhookEndpointResponse disable(@PathVariable UUID id) {
    return webhookEndpointService.disable(id);
  }

  @Operation(summary = "List webhook delivery attempts for endpoint")
  @GetMapping("/{id}/deliveries")
  public List<Map<String, Object>> deliveries(@PathVariable UUID id) {
    return webhookEndpointService.deliveries(id).stream().map(delivery -> Map.<String, Object>of(
      "id", delivery.getId(),
      "eventId", delivery.getEventId(),
      "endpointId", delivery.getEndpointId(),
      "attempt", delivery.getAttempt(),
      "status", delivery.getStatus(),
      "responseCode", delivery.getResponseCode() == null ? "" : delivery.getResponseCode(),
      "errorMessage", delivery.getErrorMessage() == null ? "" : delivery.getErrorMessage(),
      "createdAt", delivery.getCreatedAt()
    )).toList();
  }

  @Operation(summary = "Replay webhook event")
  @PostMapping("/{id}/replay")
  public void replay(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
    String eventId = payload.get("eventId");
    webhookEndpointService.replay(id, UUID.fromString(eventId));
  }
}
