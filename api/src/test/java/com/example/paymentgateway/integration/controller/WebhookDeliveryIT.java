package com.example.paymentgateway.integration.controller;

import com.example.paymentgateway.dto.WebhookEndpointRequest;
import com.example.paymentgateway.integration.support.IntegrationTestBase;
import com.example.paymentgateway.repository.OutboxEventRepository;
import com.example.paymentgateway.service.OutboxService;
import com.example.paymentgateway.service.WebhookEndpointService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class WebhookDeliveryIT extends IntegrationTestBase {
  @Autowired
  private WebhookEndpointService webhookEndpointService;

  @Autowired
  private OutboxService outboxService;

  @Autowired
  private OutboxEventRepository outboxEventRepository;

  @Test
  void shouldRetryAndEventuallyDeliverWebhook() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setResponseCode(500));
      server.enqueue(new MockResponse().setResponseCode(200));
      server.start();

      WebhookEndpointRequest request = new WebhookEndpointRequest();
      request.setUrl(server.url("/events").toString());
      request.setSecret("integration-secret-12345");
      webhookEndpointService.create(request);

      outboxService.enqueue("Payment", "a1", "PAYMENT_SUCCESS", "{\"id\":\"evt-1\"}");
      outboxService.dispatchPending();
      outboxService.dispatchPending();

      assertThat(server.getRequestCount()).isGreaterThanOrEqualTo(2);
      assertThat(outboxEventRepository.findAll()).isNotEmpty();
    }
  }
}
