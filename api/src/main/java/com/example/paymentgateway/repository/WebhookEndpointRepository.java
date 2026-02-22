package com.example.paymentgateway.repository;

import com.example.paymentgateway.domain.entity.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {
  List<WebhookEndpoint> findByActiveTrue();
}
