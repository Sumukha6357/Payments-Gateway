package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.entity.IdempotencyKey;
import com.example.paymentgateway.domain.enumtype.IdempotencyStatus;
import com.example.paymentgateway.exception.IdempotencyConflictException;
import com.example.paymentgateway.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class IdempotencyService {
  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;

  public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository,
                            ObjectMapper objectMapper,
                            MeterRegistry meterRegistry) {
    this.idempotencyKeyRepository = idempotencyKeyRepository;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
  }

  @Transactional
  public <T> IdempotencyResult<T> execute(String key,
                                          String requestFingerprint,
                                          Class<T> responseType,
                                          Supplier<IdempotencyResult<T>> supplier) {
    Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByKey(key);
    if (existing.isPresent()) {
      IdempotencyKey record = existing.get();
      if (!record.getRequestFingerprint().equals(requestFingerprint)) {
        meterRegistry.counter("idempotency_conflict_rate_total").increment();
        throw new IdempotencyConflictException("Idempotency key reused with different request payload");
      }
      if (record.getStatus() == IdempotencyStatus.COMPLETED && record.getResponsePayload() != null) {
        T replayed = fromJson(record.getResponsePayload(), responseType);
        return new IdempotencyResult<>(record.getResponseStatus(), replayed, true);
      }
      meterRegistry.counter("idempotency_conflict_rate_total").increment();
      throw new IdempotencyConflictException("Idempotency key is already being processed");
    }

    IdempotencyKey record = new IdempotencyKey();
    record.setKey(key);
    record.setRequestFingerprint(requestFingerprint);
    record.setStatus(IdempotencyStatus.IN_PROGRESS);
    try {
      idempotencyKeyRepository.save(record);
    } catch (DataIntegrityViolationException ex) {
      meterRegistry.counter("idempotency_conflict_rate_total").increment();
      throw new IdempotencyConflictException("Idempotency key conflict");
    }

    try {
      IdempotencyResult<T> response = supplier.get();
      record.setStatus(IdempotencyStatus.COMPLETED);
      record.setResponseStatus(response.getStatus());
      record.setResponsePayload(toJson(response.getBody()));
      return response;
    } catch (RuntimeException ex) {
      record.setStatus(IdempotencyStatus.FAILED);
      throw ex;
    }
  }

  public String fingerprint(String method, String path, String payload) {
    String canonicalPayload = payload == null ? "" : payload;
    return method + "|" + path + "|" + Integer.toHexString(canonicalPayload.hashCode());
  }

  private <T> T fromJson(String payload, Class<T> responseType) {
    try {
      return objectMapper.readValue(payload, responseType);
    } catch (IOException e) {
      throw new IdempotencyConflictException("Failed to read idempotent response");
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }
}
