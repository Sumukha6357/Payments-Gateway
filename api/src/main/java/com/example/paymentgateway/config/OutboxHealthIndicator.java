package com.example.paymentgateway.config;

import com.example.paymentgateway.domain.enumtype.OutboxStatus;
import com.example.paymentgateway.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("outbox")
public class OutboxHealthIndicator implements HealthIndicator {
  private final OutboxEventRepository outboxEventRepository;
  private final long backlogWarningThreshold;

  public OutboxHealthIndicator(OutboxEventRepository outboxEventRepository,
                               @Value("${outbox.backlog-warning-threshold:500}") long backlogWarningThreshold) {
    this.outboxEventRepository = outboxEventRepository;
    this.backlogWarningThreshold = backlogWarningThreshold;
  }

  @Override
  public Health health() {
    long pending = outboxEventRepository.countByStatus(OutboxStatus.PENDING);
    long processing = outboxEventRepository.countByStatus(OutboxStatus.PROCESSING);
    long backlog = pending + processing;
    if (backlog > backlogWarningThreshold) {
      return Health.down()
        .withDetail("backlog", backlog)
        .withDetail("threshold", backlogWarningThreshold)
        .build();
    }
    return Health.up()
      .withDetail("backlog", backlog)
      .withDetail("threshold", backlogWarningThreshold)
      .build();
  }
}
