package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.entity.OutboxEvent;
import com.example.paymentgateway.domain.enumtype.OutboxStatus;
import com.example.paymentgateway.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.OffsetDateTime;

@Service
public class OutboxService {
  private static final Logger logger = LoggerFactory.getLogger(OutboxService.class);

  private final OutboxEventRepository outboxEventRepository;
  private final WebhookDeliveryService webhookDeliveryService;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;

  public OutboxService(OutboxEventRepository outboxEventRepository,
                       WebhookDeliveryService webhookDeliveryService,
                       ObjectMapper objectMapper,
                       MeterRegistry meterRegistry) {
    this.outboxEventRepository = outboxEventRepository;
    this.webhookDeliveryService = webhookDeliveryService;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;

    Gauge.builder("outbox_backlog_size", this::backlogSize)
      .description("Number of pending + processing outbox events")
      .register(meterRegistry);
  }

  public void enqueue(String aggregateType, String aggregateId, String eventType, Object payload) {
    OutboxEvent event = new OutboxEvent();
    event.setAggregateType(aggregateType);
    event.setAggregateId(aggregateId);
    event.setEventType(eventType);
    event.setPayload(toJson(payload));
    event.setStatus(OutboxStatus.PENDING);
    outboxEventRepository.save(event);
  }

  @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:5000}")
  @Transactional
  public void dispatchPending() {
    List<OutboxEvent> pending = outboxEventRepository.findDispatchable(
      List.of(OutboxStatus.PENDING, OutboxStatus.PROCESSING), OffsetDateTime.now());
    for (OutboxEvent event : pending) {
      event.setStatus(OutboxStatus.PROCESSING);
      event.setAttemptCount(event.getAttemptCount() + 1);
      publish(event);
    }
  }

  private void publish(OutboxEvent event) {
    WebhookDeliveryService.DeliveryResult result = webhookDeliveryService.deliver(event);
    if (result.success()) {
      event.setStatus(OutboxStatus.SENT);
      event.setPublishedAt(OffsetDateTime.now());
      event.setLastError(null);
      logger.info("Outbox event published: id={}, aggregateType={}, eventType={}",
        event.getId(), event.getAggregateType(), event.getEventType());
      return;
    }

    int backoffSeconds = (int) Math.min(300, Math.pow(2, Math.max(1, event.getAttemptCount())));
    event.setStatus(event.getAttemptCount() >= 8 ? OutboxStatus.FAILED : OutboxStatus.PENDING);
    event.setNextAttemptAt(OffsetDateTime.now().plusSeconds(backoffSeconds));
    event.setLastError(result.errorMessage());
    logger.warn("Outbox publish failed: id={}, attempt={}, nextAttemptAt={}, error={}",
      event.getId(), event.getAttemptCount(), event.getNextAttemptAt(), result.errorMessage());
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  private double backlogSize() {
    return outboxEventRepository.countByStatus(OutboxStatus.PENDING)
      + outboxEventRepository.countByStatus(OutboxStatus.PROCESSING);
  }
}
