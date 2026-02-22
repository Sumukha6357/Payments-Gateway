package com.example.paymentgateway.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SecretValueResolver {
  private SecretValueResolver() {
  }

  public static String resolve(String envKey, String configuredValue) {
    if (configuredValue != null && !configuredValue.isBlank()) {
      return configuredValue;
    }
    String direct = System.getenv(envKey);
    if (direct != null && !direct.isBlank()) {
      return direct;
    }
    String fromFile = System.getenv(envKey + "_FILE");
    if (fromFile != null && !fromFile.isBlank()) {
      try {
        return Files.readString(Path.of(fromFile), StandardCharsets.UTF_8).trim();
      } catch (IOException ex) {
        throw new IllegalStateException("Failed to read secret from " + envKey + "_FILE", ex);
      }
    }
    return "";
  }
}
