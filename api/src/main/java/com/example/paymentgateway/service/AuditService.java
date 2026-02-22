package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.entity.AuditLog;
import com.example.paymentgateway.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditLogRepository auditLogRepository;

  public AuditService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  public void log(String entityType, String entityId, String action, String payload) {
    AuditLog log = new AuditLog();
    log.setEntityType(entityType);
    log.setEntityId(entityId);
    log.setAction(action);
    log.setPayload(maskSensitive(payload));
    auditLogRepository.save(log);
  }

  private String maskSensitive(String payload) {
    if (payload == null) {
      return "{}";
    }
    return payload
      .replaceAll("(?i)\"secret\"\\s*:\\s*\"[^\"]+\"", "\"secret\":\"***\"")
      .replaceAll("(?i)\"token\"\\s*:\\s*\"[^\"]+\"", "\"token\":\"***\"");
  }
}
