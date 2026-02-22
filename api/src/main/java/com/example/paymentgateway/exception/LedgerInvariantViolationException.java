package com.example.paymentgateway.exception;

public class LedgerInvariantViolationException extends RuntimeException {
  public LedgerInvariantViolationException(String message) {
    super(message);
  }
}
