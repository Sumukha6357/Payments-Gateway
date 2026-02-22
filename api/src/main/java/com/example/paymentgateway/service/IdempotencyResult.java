package com.example.paymentgateway.service;

public class IdempotencyResult<T> {
  private final int status;
  private final T body;
  private final boolean replayed;

  public IdempotencyResult(int status, T body, boolean replayed) {
    this.status = status;
    this.body = body;
    this.replayed = replayed;
  }

  public int getStatus() {
    return status;
  }

  public T getBody() {
    return body;
  }

  public boolean isReplayed() {
    return replayed;
  }
}
