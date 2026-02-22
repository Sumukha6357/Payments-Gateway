package com.example.paymentgateway.unit.service;

import com.example.paymentgateway.domain.entity.IdempotencyKey;
import com.example.paymentgateway.domain.enumtype.IdempotencyStatus;
import com.example.paymentgateway.exception.IdempotencyConflictException;
import com.example.paymentgateway.repository.IdempotencyKeyRepository;
import com.example.paymentgateway.service.IdempotencyResult;
import com.example.paymentgateway.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class IdempotencyServiceTest {
  @Test
  void shouldReplayStoredResponseForSameFingerprint() {
    IdempotencyKeyRepository repository = Mockito.mock(IdempotencyKeyRepository.class);
    IdempotencyService service = new IdempotencyService(repository, new ObjectMapper(), new SimpleMeterRegistry());

    IdempotencyKey record = new IdempotencyKey();
    record.setKey("k1");
    record.setRequestFingerprint("fp");
    record.setStatus(IdempotencyStatus.COMPLETED);
    record.setResponseStatus(200);
    record.setResponsePayload("{\"value\":\"ok\"}");
    when(repository.findByKey("k1")).thenReturn(Optional.of(record));

    var result = service.execute("k1", "fp", Response.class, () -> new IdempotencyResult<>(200, new Response("new"), false));
    assertThat(result.isReplayed()).isTrue();
    assertThat(result.getBody().value()).isEqualTo("ok");
  }

  @Test
  void shouldRejectSameKeyWithDifferentFingerprint() {
    IdempotencyKeyRepository repository = Mockito.mock(IdempotencyKeyRepository.class);
    IdempotencyService service = new IdempotencyService(repository, new ObjectMapper(), new SimpleMeterRegistry());

    IdempotencyKey record = new IdempotencyKey();
    record.setKey("k1");
    record.setRequestFingerprint("fp-a");
    record.setStatus(IdempotencyStatus.COMPLETED);
    when(repository.findByKey("k1")).thenReturn(Optional.of(record));

    assertThatThrownBy(() -> service.execute("k1", "fp-b", Response.class,
      () -> new IdempotencyResult<>(200, new Response("new"), false)))
      .isInstanceOf(IdempotencyConflictException.class);
  }

  @Test
  void shouldPersistNewExecutionResult() {
    IdempotencyKeyRepository repository = Mockito.mock(IdempotencyKeyRepository.class);
    IdempotencyService service = new IdempotencyService(repository, new ObjectMapper(), new SimpleMeterRegistry());
    when(repository.findByKey("k2")).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.execute("k2", "fp", Response.class, () -> new IdempotencyResult<>(201, new Response("created"), false));
    assertThat(result.getStatus()).isEqualTo(201);
    assertThat(result.getBody().value()).isEqualTo("created");
  }

  record Response(String value) {}
}
