package com.example.paymentgateway.security;

import com.example.paymentgateway.config.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  private final CorrelationIdFilter correlationIdFilter;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RateLimitingFilter rateLimitingFilter;
  private final boolean requireHttps;

  public SecurityConfig(CorrelationIdFilter correlationIdFilter,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        RateLimitingFilter rateLimitingFilter,
                        @Value("${security.require-https:false}") boolean requireHttps) {
    this.correlationIdFilter = correlationIdFilter;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.rateLimitingFilter = rateLimitingFilter;
    this.requireHttps = requireHttps;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .requiresChannel(channel -> {
        if (requireHttps) {
          channel.anyRequest().requiresSecure();
        }
      })
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/actuator/health/**").permitAll()
        .requestMatchers("/payments/webhook").permitAll()
        .requestMatchers("/auth/login", "/auth/refresh", "/auth/logout").permitAll()
        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
      )
      .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
      .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
      .addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class);
    return http.build();
  }
}
