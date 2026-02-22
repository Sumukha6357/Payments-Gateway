package com.example.paymentgateway.security;

import com.example.paymentgateway.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
  private static final String TRANSFER_PATH = "/transactions/transfer";
  private static final String PAYMENTS_PATH = "/payments";
  private static final String WEBHOOK_PATH = "/payments/webhook";

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final Map<String, Bucket> webhookIpBuckets = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper;

  public RateLimitingFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
    if (isWebhookRequest(request)) {
      String ipKey = "webhook-ip:" + resolveClientIp(request);
      Bucket ipBucket = webhookIpBuckets.computeIfAbsent(ipKey, k -> webhookIpBucket());
      if (!ipBucket.tryConsume(1)) {
        ApiError error = ApiError.of(429, "WEBHOOK_RATE_LIMITED", "Webhook source is rate limited", request.getRequestURI());
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
        return;
      }
    }

    if (shouldRateLimit(request)) {
      String key = resolveUserKey();
      Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());
      if (!bucket.tryConsume(1)) {
        ApiError error = ApiError.of(429, "RATE_LIMITED", "Too many requests", request.getRequestURI());
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  private boolean shouldRateLimit(HttpServletRequest request) {
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return false;
    }
    String path = request.getRequestURI();
    return TRANSFER_PATH.equals(path) || PAYMENTS_PATH.equals(path);
  }

  private boolean isWebhookRequest(HttpServletRequest request) {
    return "POST".equalsIgnoreCase(request.getMethod()) && WEBHOOK_PATH.equals(request.getRequestURI());
  }

  private String resolveUserKey() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      return authentication.getName();
    }
    return "anonymous";
  }

  private Bucket newBucket() {
    Refill refill = Refill.intervally(5, Duration.ofMinutes(1));
    Bandwidth limit = Bandwidth.classic(5, refill);
    return Bucket.builder().addLimit(limit).build();
  }

  private Bucket webhookIpBucket() {
    Refill refill = Refill.intervally(20, Duration.ofMinutes(1));
    Bandwidth limit = Bandwidth.classic(20, refill);
    return Bucket.builder().addLimit(limit).build();
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
