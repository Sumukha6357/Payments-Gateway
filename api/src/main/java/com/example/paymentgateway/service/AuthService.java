package com.example.paymentgateway.service;

import com.example.paymentgateway.dto.AuthTokenResponse;
import com.example.paymentgateway.security.JwtService;
import com.example.paymentgateway.security.SecretValueResolver;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

@Service
public class AuthService {
  private final String adminUsername;
  private final String adminPassword;
  private final JwtService jwtService;
  private final Duration accessTokenTtl;
  private final Duration refreshTokenTtl;

  public AuthService(@Value("${security.auth.admin-username:}") String adminUsername,
                     @Value("${security.auth.admin-password:}") String adminPassword,
                     @Value("${security.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl,
                     @Value("${security.jwt.refresh-token-ttl:P7D}") Duration refreshTokenTtl,
                     JwtService jwtService) {
    this.adminUsername = SecretValueResolver.resolve("AUTH_ADMIN_USERNAME", adminUsername);
    this.adminPassword = SecretValueResolver.resolve("AUTH_ADMIN_PASSWORD", adminPassword);
    this.accessTokenTtl = accessTokenTtl;
    this.refreshTokenTtl = refreshTokenTtl;
    this.jwtService = jwtService;
  }

  public Tokens login(String username, String password) {
    if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    List<String> roles = List.of("ADMIN", "USER");
    String accessToken = jwtService.generateAccessToken(username, roles);
    String refreshToken = jwtService.generateRefreshToken(username, roles);
    return new Tokens(AuthTokenResponse.of(accessToken, accessTokenTtl.toSeconds()), refreshToken, refreshTokenTtl);
  }

  public Tokens refresh(String refreshToken) {
    Claims claims = jwtService.parse(refreshToken);
    if (!jwtService.isRefreshToken(claims)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }
    String subject = claims.getSubject();
    List<String> roles = jwtService.extractRoles(claims);
    String accessToken = jwtService.generateAccessToken(subject, roles);
    String rotatedRefreshToken = jwtService.generateRefreshToken(subject, roles);
    return new Tokens(AuthTokenResponse.of(accessToken, accessTokenTtl.toSeconds()), rotatedRefreshToken, refreshTokenTtl);
  }

  public record Tokens(AuthTokenResponse accessToken, String refreshToken, Duration refreshTokenTtl) {
  }
}
