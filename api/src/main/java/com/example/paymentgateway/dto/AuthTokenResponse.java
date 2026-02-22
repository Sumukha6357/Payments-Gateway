package com.example.paymentgateway.dto;

public class AuthTokenResponse {
  private String token;
  private long expiresInSeconds;

  public static AuthTokenResponse of(String token, long expiresInSeconds) {
    AuthTokenResponse response = new AuthTokenResponse();
    response.token = token;
    response.expiresInSeconds = expiresInSeconds;
    return response;
  }

  public String getToken() {
    return token;
  }

  public long getExpiresInSeconds() {
    return expiresInSeconds;
  }
}
