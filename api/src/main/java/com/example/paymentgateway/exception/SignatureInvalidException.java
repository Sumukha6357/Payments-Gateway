package com.example.paymentgateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class SignatureInvalidException extends RuntimeException {
  public SignatureInvalidException(String message) {
    super(message);
  }
}
