package com.example.paymentgateway.service;

import com.example.paymentgateway.repository.LedgerEntryRepository;
import com.example.paymentgateway.repository.projection.WalletBalanceProjection;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class LedgerReconciliationService {
  private static final Logger log = LoggerFactory.getLogger(LedgerReconciliationService.class);
  private final LedgerEntryRepository ledgerEntryRepository;
  private final JdbcTemplate jdbcTemplate;
  private final AtomicLong mismatchGauge;

  public LedgerReconciliationService(LedgerEntryRepository ledgerEntryRepository,
                                     JdbcTemplate jdbcTemplate,
                                     MeterRegistry meterRegistry) {
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.mismatchGauge = meterRegistry.gauge("ledger_reconciliation_mismatches", new AtomicLong(0));
  }

  @Scheduled(cron = "${ledger.reconciliation-cron:0 0 2 * * *}")
  public void reconcile() {
    Map<UUID, BigDecimal> ledgerBalances = ledgerEntryRepository.aggregateBalances().stream()
      .collect(Collectors.toMap(WalletBalanceProjection::getWalletId, WalletBalanceProjection::getBalance));

    Map<UUID, BigDecimal> storedBalances = jdbcTemplate.query(
      "select wallet_id, balance from wallet_balances",
      rs -> {
        Map<UUID, BigDecimal> result = new java.util.HashMap<>();
        while (rs.next()) {
          result.put((UUID) rs.getObject("wallet_id"), rs.getBigDecimal("balance"));
        }
        return result;
      });

    long mismatches = 0;
    for (Map.Entry<UUID, BigDecimal> entry : ledgerBalances.entrySet()) {
      BigDecimal stored = storedBalances.getOrDefault(entry.getKey(), BigDecimal.ZERO);
      if (entry.getValue().compareTo(stored) != 0) {
        mismatches++;
        log.error("Ledger reconciliation mismatch for wallet {} ledger={} stored={}",
          entry.getKey(), entry.getValue(), stored);
      }
    }

    mismatchGauge.set(mismatches);
    if (mismatches == 0) {
      log.info("Ledger reconciliation completed successfully");
    }
  }
}
