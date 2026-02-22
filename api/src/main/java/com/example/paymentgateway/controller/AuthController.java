package com.example.paymentgateway.controller;

import com.example.paymentgateway.dto.AuthLoginRequest;
import com.example.paymentgateway.dto.AuthTokenResponse;
import com.example.paymentgateway.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private static final String REFRESH_COOKIE = "pg_refresh_token";
  private final AuthService authService;
  private final boolean secureCookie;

  public AuthController(AuthService authService,
                        @Value("${security.auth.refresh-cookie-secure:true}") boolean secureCookie) {
    this.authService = authService;
    this.secureCookie = secureCookie;
  }

  @Operation(summary = "Login and issue JWT access token + refresh cookie")
  @PostMapping("/login")
  public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody AuthLoginRequest request, HttpServletResponse response) {
    AuthService.Tokens tokens = authService.login(request.getUsername(), request.getPassword());
    attachRefreshCookie(tokens, response);
    return ResponseEntity.ok(tokens.accessToken());
  }

  @Operation(summary = "Refresh JWT access token")
  @PostMapping("/refresh")
  public ResponseEntity<AuthTokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = readRefreshCookie(request);
    AuthService.Tokens tokens = authService.refresh(refreshToken);
    attachRefreshCookie(tokens, response);
    return ResponseEntity.ok(tokens.accessToken());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletResponse response) {
    Cookie cookie = new Cookie(REFRESH_COOKIE, "");
    cookie.setHttpOnly(true);
    cookie.setSecure(secureCookie);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
    return ResponseEntity.noContent().build();
  }

  private void attachRefreshCookie(AuthService.Tokens tokens, HttpServletResponse response) {
    Cookie cookie = new Cookie(REFRESH_COOKIE, tokens.refreshToken());
    cookie.setHttpOnly(true);
    cookie.setSecure(secureCookie);
    cookie.setPath("/");
    cookie.setMaxAge((int) tokens.refreshTokenTtl().toSeconds());
    response.addCookie(cookie);
  }

  private String readRefreshCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return "";
    }
    for (Cookie cookie : cookies) {
      if (REFRESH_COOKIE.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return "";
  }
}
