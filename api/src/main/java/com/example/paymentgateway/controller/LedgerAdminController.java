package com.example.paymentgateway.controller;

import com.example.paymentgateway.domain.entity.LedgerEntry;
import com.example.paymentgateway.repository.LedgerEntryRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/ledger")
@PreAuthorize("hasRole('ADMIN')")
public class LedgerAdminController {
  private final LedgerEntryRepository ledgerEntryRepository;

  public LedgerAdminController(LedgerEntryRepository ledgerEntryRepository) {
    this.ledgerEntryRepository = ledgerEntryRepository;
  }

  @Operation(summary = "Get ledger entries by transaction reference")
  @GetMapping("/{referenceId}")
  public List<Map<String, Object>> getByReference(@PathVariable UUID referenceId) {
    return ledgerEntryRepository.findByReferenceId(referenceId).stream()
      .map(this::toMap)
      .toList();
  }

  @Operation(summary = "List ledger entries")
  @GetMapping
  public List<Map<String, Object>> list(Pageable pageable) {
    return ledgerEntryRepository.findAll(pageable).stream().map(this::toMap).toList();
  }

  private Map<String, Object> toMap(LedgerEntry entry) {
    return Map.of(
      "id", entry.getId(),
      "walletId", entry.getWallet().getId(),
      "type", entry.getType(),
      "amount", entry.getAmount(),
      "referenceId", entry.getReferenceId(),
      "createdAt", entry.getCreatedAt()
    );
  }
}
