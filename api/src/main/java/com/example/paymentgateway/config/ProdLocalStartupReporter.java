package com.example.paymentgateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("prod-local")
public class ProdLocalStartupReporter {
  private static final Logger log = LoggerFactory.getLogger(ProdLocalStartupReporter.class);

  private final String datasourceUrl;
  private final String redisHost;
  private final String redisPort;
  private final boolean requireHttps;
  private final boolean enforceSecretValidation;
  private final String accessTokenTtl;
  private final String refreshTokenTtl;
  private final String corsOrigins;

  public ProdLocalStartupReporter(@Value("${spring.datasource.url}") String datasourceUrl,
                                  @Value("${spring.data.redis.host}") String redisHost,
                                  @Value("${spring.data.redis.port}") String redisPort,
                                  @Value("${security.require-https}") boolean requireHttps,
                                  @Value("${security.enforce-secret-validation}") boolean enforceSecretValidation,
                                  @Value("${security.jwt.access-token-ttl}") String accessTokenTtl,
                                  @Value("${security.jwt.refresh-token-ttl}") String refreshTokenTtl,
                                  @Value("${security.cors.allowed-origins}") String corsOrigins) {
    this.datasourceUrl = datasourceUrl;
    this.redisHost = redisHost;
    this.redisPort = redisPort;
    this.requireHttps = requireHttps;
    this.enforceSecretValidation = enforceSecretValidation;
    this.accessTokenTtl = accessTokenTtl;
    this.refreshTokenTtl = refreshTokenTtl;
    this.corsOrigins = corsOrigins;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void report() {
    log.info("prod-local active configuration: datasourceUrl={}, redis={}:{}, requireHttps={}, strictSecretValidation={}, accessTokenTtl={}, refreshTokenTtl={}, corsOrigins={}",
      datasourceUrl, redisHost, redisPort, requireHttps, enforceSecretValidation, accessTokenTtl, refreshTokenTtl, corsOrigins);
  }
}
