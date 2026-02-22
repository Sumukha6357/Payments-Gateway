package com.example.paymentgateway.exception;

import java.time.OffsetDateTime;

public class ApiError {
  private OffsetDateTime timestamp;
  private int status;
  private String errorCode;
  private String message;
  private String path;
  private String correlationId;

  public static ApiError of(int status, String errorCode, String message, String path) {
    return of(status, errorCode, message, path, null);
  }

  public static ApiError of(int status, String errorCode, String message, String path, String correlationId) {
    ApiError error = new ApiError();
    error.timestamp = OffsetDateTime.now();
    error.status = status;
    error.errorCode = errorCode;
    error.message = message;
    error.path = path;
    error.correlationId = correlationId;
    return error;
  }

  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public int getStatus() {
    return status;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getMessage() {
    return message;
  }

  public String getPath() {
    return path;
  }

  public String getCorrelationId() {
    return correlationId;
  }
}
