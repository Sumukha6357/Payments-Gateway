package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.entity.WebhookEndpoint;
import com.example.paymentgateway.domain.enumtype.OutboxStatus;
import com.example.paymentgateway.dto.WebhookEndpointRequest;
import com.example.paymentgateway.dto.WebhookEndpointResponse;
import com.example.paymentgateway.exception.NotFoundException;
import com.example.paymentgateway.repository.OutboxEventRepository;
import com.example.paymentgateway.repository.WebhookDeliveryRepository;
import com.example.paymentgateway.repository.WebhookEndpointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WebhookEndpointService {
  private final WebhookEndpointRepository webhookEndpointRepository;
  private final WebhookDeliveryRepository webhookDeliveryRepository;
  private final OutboxEventRepository outboxEventRepository;
  private final AuditService auditService;

  public WebhookEndpointService(WebhookEndpointRepository webhookEndpointRepository,
                                WebhookDeliveryRepository webhookDeliveryRepository,
                                OutboxEventRepository outboxEventRepository,
                                AuditService auditService) {
    this.webhookEndpointRepository = webhookEndpointRepository;
    this.webhookDeliveryRepository = webhookDeliveryRepository;
    this.outboxEventRepository = outboxEventRepository;
    this.auditService = auditService;
  }

  @Transactional
  public WebhookEndpointResponse create(WebhookEndpointRequest request) {
    WebhookEndpoint endpoint = new WebhookEndpoint();
    endpoint.setUrl(request.getUrl());
    endpoint.setSecret(request.getSecret());
    endpoint.setActive(true);
    WebhookEndpoint saved = webhookEndpointRepository.save(endpoint);
    auditService.log("WebhookEndpoint", saved.getId().toString(), "CREATE", "{\"url\":\"" + request.getUrl() + "\"}");
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<WebhookEndpointResponse> list() {
    return webhookEndpointRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public WebhookEndpointResponse disable(UUID id) {
    WebhookEndpoint endpoint = webhookEndpointRepository.findById(id)
      .orElseThrow(() -> new NotFoundException("Webhook endpoint not found"));
    endpoint.setActive(false);
    auditService.log("WebhookEndpoint", endpoint.getId().toString(), "DISABLE", "{}");
    return toResponse(endpoint);
  }

  @Transactional(readOnly = true)
  public List<com.example.paymentgateway.domain.entity.WebhookDelivery> deliveries(UUID endpointId) {
    return webhookDeliveryRepository.findByEndpointIdOrderByCreatedAtDesc(endpointId);
  }

  @Transactional
  public void replay(UUID endpointId, UUID eventId) {
    webhookEndpointRepository.findById(endpointId)
      .orElseThrow(() -> new NotFoundException("Webhook endpoint not found"));
    var event = outboxEventRepository.findById(eventId)
      .orElseThrow(() -> new NotFoundException("Outbox event not found"));
    event.setStatus(OutboxStatus.PENDING);
    event.setNextAttemptAt(java.time.OffsetDateTime.now());
    auditService.log("WebhookDelivery", eventId.toString(), "REPLAY", "{\"endpointId\":\"" + endpointId + "\"}");
  }

  private WebhookEndpointResponse toResponse(WebhookEndpoint endpoint) {
    WebhookEndpointResponse response = new WebhookEndpointResponse();
    response.setId(endpoint.getId());
    response.setUrl(endpoint.getUrl());
    response.setActive(endpoint.isActive());
    response.setCreatedAt(endpoint.getCreatedAt());
    return response;
  }
}
