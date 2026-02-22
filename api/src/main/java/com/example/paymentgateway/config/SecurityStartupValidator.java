package com.example.paymentgateway.config;

import com.example.paymentgateway.security.SecretValueResolver;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityStartupValidator {
  private final String jwtSecret;
  private final String webhookSecret;
  private final String authAdminUsername;
  private final String authAdminPassword;
  private final boolean enforceStrictSecrets;

  public SecurityStartupValidator(@Value("${security.jwt.secret:}") String jwtSecret,
                                  @Value("${payments.webhook.secret:}") String webhookSecret,
                                  @Value("${security.auth.admin-username:}") String authAdminUsername,
                                  @Value("${security.auth.admin-password:}") String authAdminPassword,
                                  @Value("${security.enforce-secret-validation:true}") boolean enforceStrictSecrets) {
    this.jwtSecret = SecretValueResolver.resolve("JWT_SECRET", jwtSecret);
    this.webhookSecret = SecretValueResolver.resolve("WEBHOOK_SECRET", webhookSecret);
    this.authAdminUsername = SecretValueResolver.resolve("AUTH_ADMIN_USERNAME", authAdminUsername);
    this.authAdminPassword = SecretValueResolver.resolve("AUTH_ADMIN_PASSWORD", authAdminPassword);
    this.enforceStrictSecrets = enforceStrictSecrets;
  }

  @PostConstruct
  void validate() {
    if (!enforceStrictSecrets) {
      return;
    }
    ensureNotBlank(jwtSecret, "JWT_SECRET");
    ensureNotBlank(webhookSecret, "WEBHOOK_SECRET");
    ensureNotBlank(authAdminUsername, "AUTH_ADMIN_USERNAME");
    ensureNotBlank(authAdminPassword, "AUTH_ADMIN_PASSWORD");

    if (jwtSecret.contains("change-me") || webhookSecret.contains("change-me")) {
      throw new IllegalStateException("Default secrets are not allowed in production mode");
    }
  }

  private void ensureNotBlank(String value, String key) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing required secret: " + key);
    }
  }
}
