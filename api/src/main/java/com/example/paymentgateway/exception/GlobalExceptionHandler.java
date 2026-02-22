package com.example.paymentgateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.MDC;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(IdempotencyConflictException.class)
  public ResponseEntity<ApiError> handleIdempotencyConflict(IdempotencyConflictException ex, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(LedgerInvariantViolationException.class)
  public ResponseEntity<ApiError> handleLedgerViolation(LedgerInvariantViolationException ex, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, "LEDGER_INVARIANT_VIOLATION", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message = ex.getBindingResult().getAllErrors().isEmpty()
      ? "Validation failed"
      : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
    return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message, request.getRequestURI());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
    return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), request.getRequestURI());
  }

  private ResponseEntity<ApiError> buildResponse(HttpStatus status, String errorCode, String message, String path) {
    ApiError error = ApiError.of(status.value(), errorCode, message, path, MDC.get("correlationId"));
    return ResponseEntity.status(status).body(error);
  }
}
