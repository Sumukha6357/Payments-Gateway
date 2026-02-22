package com.example.paymentgateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtService {
  private final Key key;
  private final String issuer;
  private final String audience;
  private final Duration accessTokenTtl;
  private final Duration refreshTokenTtl;

  public JwtService(@Value("${security.jwt.secret:}") String secret,
                    @Value("${security.jwt.issuer}") String issuer,
                    @Value("${security.jwt.audience}") String audience,
                    @Value("${security.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl,
                    @Value("${security.jwt.refresh-token-ttl:P7D}") Duration refreshTokenTtl) {
    String resolvedSecret = SecretValueResolver.resolve("JWT_SECRET", secret);
    if (resolvedSecret.length() < 32) {
      throw new IllegalStateException("JWT secret must be at least 32 characters");
    }
    this.key = Keys.hmacShaKeyFor(resolvedSecret.getBytes(StandardCharsets.UTF_8));
    this.issuer = issuer;
    this.audience = audience;
    this.accessTokenTtl = accessTokenTtl;
    this.refreshTokenTtl = refreshTokenTtl;
  }

  public Claims parse(String token) {
    Claims claims = Jwts.parserBuilder()
      .setSigningKey(key)
      .requireIssuer(issuer)
      .build()
      .parseClaimsJws(token)
      .getBody();
    String tokenAudience = claims.getAudience();
    if (tokenAudience == null || !tokenAudience.equals(audience)) {
      throw new SignatureException("Invalid JWT audience");
    }
    return claims;
  }

  public String generateAccessToken(String subject, List<String> roles) {
    Instant now = Instant.now();
    return Jwts.builder()
      .setSubject(subject)
      .setIssuer(issuer)
      .setAudience(audience)
      .claim("roles", roles)
      .claim("type", "access")
      .setIssuedAt(Date.from(now))
      .setExpiration(Date.from(now.plus(accessTokenTtl)))
      .signWith(key)
      .compact();
  }

  public String generateRefreshToken(String subject, List<String> roles) {
    Instant now = Instant.now();
    return Jwts.builder()
      .setSubject(subject)
      .setIssuer(issuer)
      .setAudience(audience)
      .claim("roles", roles)
      .claim("type", "refresh")
      .setIssuedAt(Date.from(now))
      .setExpiration(Date.from(now.plus(refreshTokenTtl)))
      .signWith(key)
      .compact();
  }

  public boolean isRefreshToken(Claims claims) {
    Object tokenType = claims.get("type");
    return "refresh".equals(tokenType);
  }

  public List<String> extractRoles(Claims claims) {
    Object roles = claims.get("roles");
    if (roles instanceof List) {
      @SuppressWarnings("unchecked")
      List<String> list = (List<String>) roles;
      return list;
    }
    return List.of();
  }
}
