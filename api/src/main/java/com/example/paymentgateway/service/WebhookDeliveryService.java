package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.entity.OutboxEvent;
import com.example.paymentgateway.domain.entity.WebhookDelivery;
import com.example.paymentgateway.domain.entity.WebhookEndpoint;
import com.example.paymentgateway.repository.WebhookDeliveryRepository;
import com.example.paymentgateway.repository.WebhookEndpointRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebhookDeliveryService {
  private final WebhookEndpointRepository webhookEndpointRepository;
  private final WebhookDeliveryRepository webhookDeliveryRepository;
  private final HmacSignatureService hmacSignatureService;
  private final MeterRegistry meterRegistry;
  private final RestTemplate restTemplate;
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final int requestsPerMinute;

  public WebhookDeliveryService(WebhookEndpointRepository webhookEndpointRepository,
                                WebhookDeliveryRepository webhookDeliveryRepository,
                                HmacSignatureService hmacSignatureService,
                                MeterRegistry meterRegistry,
                                RestTemplateBuilder restTemplateBuilder,
                                @Value("${webhooks.rate-limit-per-minute:60}") int requestsPerMinute) {
    this.webhookEndpointRepository = webhookEndpointRepository;
    this.webhookDeliveryRepository = webhookDeliveryRepository;
    this.hmacSignatureService = hmacSignatureService;
    this.meterRegistry = meterRegistry;
    this.restTemplate = restTemplateBuilder
      .setConnectTimeout(Duration.ofSeconds(3))
      .setReadTimeout(Duration.ofSeconds(5))
      .build();
    this.requestsPerMinute = requestsPerMinute;
  }

  public DeliveryResult deliver(OutboxEvent event) {
    DeliveryResult result = new DeliveryResult(true, null);
    for (WebhookEndpoint endpoint : webhookEndpointRepository.findByActiveTrue()) {
      if (!tryConsume(endpoint.getId().toString())) {
        storeAttempt(event, endpoint, "RATE_LIMITED", 429, "Per-endpoint delivery rate limit exceeded");
        meterRegistry.counter("webhook_failed_total").increment();
        result = new DeliveryResult(false, "Rate limited");
        continue;
      }

      String timestamp = OffsetDateTime.now().toString();
      String payloadToSign = event.getPayload() + "|" + timestamp;
      String signature = hmacSignatureService.sign(payloadToSign, endpoint.getSecret());
      HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Type", "application/json");
      headers.add("X-Signature", signature);
      headers.add("X-Event-Id", event.getId().toString());
      headers.add("X-Event-Type", event.getEventType());
      headers.add("X-Timestamp", timestamp);

      try {
        meterRegistry.counter("webhook_attempts_total").increment();
        ResponseEntity<String> response = restTemplate.exchange(endpoint.getUrl(), HttpMethod.POST,
          new HttpEntity<>(event.getPayload(), headers), String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
          storeAttempt(event, endpoint, "DELIVERED", response.getStatusCode().value(), null);
          meterRegistry.counter("webhook_delivered_total").increment();
        } else {
          storeAttempt(event, endpoint, "FAILED", response.getStatusCode().value(), "Non-success response");
          meterRegistry.counter("webhook_failed_total").increment();
          result = new DeliveryResult(false, "Non-success response");
        }
      } catch (RestClientException ex) {
        storeAttempt(event, endpoint, "FAILED", null, ex.getMessage());
        meterRegistry.counter("webhook_failed_total").increment();
        result = new DeliveryResult(false, ex.getMessage());
      }
    }
    return result;
  }

  private boolean tryConsume(String key) {
    Bucket bucket = buckets.computeIfAbsent(key, k -> {
      Refill refill = Refill.intervally(requestsPerMinute, Duration.ofMinutes(1));
      Bandwidth bandwidth = Bandwidth.classic(requestsPerMinute, refill);
      return Bucket.builder().addLimit(bandwidth).build();
    });
    return bucket.tryConsume(1);
  }

  private void storeAttempt(OutboxEvent event, WebhookEndpoint endpoint, String status, Integer responseCode, String error) {
    WebhookDelivery delivery = new WebhookDelivery();
    delivery.setEventId(event.getId());
    delivery.setEndpointId(endpoint.getId());
    delivery.setAttempt(event.getAttemptCount() + 1);
    delivery.setStatus(status);
    delivery.setResponseCode(responseCode);
    delivery.setErrorMessage(error);
    webhookDeliveryRepository.save(delivery);
  }

  public record DeliveryResult(boolean success, String errorMessage) {
  }
}
