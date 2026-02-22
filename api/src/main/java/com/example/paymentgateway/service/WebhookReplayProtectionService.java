package com.example.paymentgateway.service;

import com.example.paymentgateway.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebhookReplayProtectionService {
  private final StringRedisTemplate redisTemplate;
  private final Duration maxClockSkew;
  private final Map<String, Instant> inMemoryCache = new ConcurrentHashMap<>();

  public WebhookReplayProtectionService(StringRedisTemplate redisTemplate,
                                        @Value("${payments.webhook.max-clock-skew:PT5M}") Duration maxClockSkew) {
    this.redisTemplate = redisTemplate;
    this.maxClockSkew = maxClockSkew;
  }

  public void assertFresh(String eventId, String timestamp) {
    if (eventId == null || eventId.isBlank()) {
      throw new BadRequestException("X-Event-Id header required");
    }
    if (timestamp == null || timestamp.isBlank()) {
      throw new BadRequestException("X-Timestamp header required");
    }

    Instant sentAt;
    try {
      sentAt = OffsetDateTime.parse(timestamp).toInstant();
    } catch (DateTimeParseException ex) {
      throw new BadRequestException("Invalid X-Timestamp format");
    }

    Instant now = Instant.now();
    if (sentAt.isBefore(now.minus(maxClockSkew)) || sentAt.isAfter(now.plus(maxClockSkew))) {
      throw new BadRequestException("Webhook timestamp outside accepted clock skew");
    }

    String key = "webhook:replay:" + eventId;
    boolean replayDetected = !markOnce(key, maxClockSkew.plusMinutes(1));
    if (replayDetected) {
      throw new BadRequestException("Webhook replay detected");
    }
  }

  private boolean markOnce(String key, Duration ttl) {
    try {
      Boolean inserted = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
      return Boolean.TRUE.equals(inserted);
    } catch (Exception ex) {
      Instant expiresAt = Instant.now().plus(ttl);
      Instant existing = inMemoryCache.putIfAbsent(key, expiresAt);
      if (existing != null && existing.isAfter(Instant.now())) {
        return false;
      }
      inMemoryCache.put(key, expiresAt);
      return true;
    }
  }
}
