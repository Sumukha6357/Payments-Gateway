package com.example.paymentgateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WebhookEndpointRequest {
  @NotBlank
  @Size(max = 512)
  private String url;

  @NotBlank
  @Size(min = 16, max = 255)
  private String secret;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }
}
