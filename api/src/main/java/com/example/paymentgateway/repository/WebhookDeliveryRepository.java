package com.example.paymentgateway.repository;

import com.example.paymentgateway.domain.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
  List<WebhookDelivery> findByEndpointIdOrderByCreatedAtDesc(UUID endpointId);
}
