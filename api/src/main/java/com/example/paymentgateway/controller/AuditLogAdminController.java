package com.example.paymentgateway.controller;

import com.example.paymentgateway.domain.entity.AuditLog;
import com.example.paymentgateway.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogAdminController {
  private final AuditLogRepository auditLogRepository;

  public AuditLogAdminController(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Operation(summary = "List audit logs")
  @GetMapping
  public List<AuditLog> list(@RequestParam(name = "q", required = false) String q,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             @RequestParam(name = "size", defaultValue = "50") int size) {
    Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200));
    if (q == null || q.isBlank()) {
      return auditLogRepository.findAll(pageable).getContent();
    }
    Specification<AuditLog> spec = (root, query, cb) -> cb.or(
      cb.like(cb.lower(root.get("entityType")), "%" + q.toLowerCase() + "%"),
      cb.like(cb.lower(root.get("entityId")), "%" + q.toLowerCase() + "%"),
      cb.like(cb.lower(root.get("action")), "%" + q.toLowerCase() + "%")
    );
    return auditLogRepository.findAll(spec, pageable).getContent();
  }
}
