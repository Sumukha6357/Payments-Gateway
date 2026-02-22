package com.example.paymentgateway.repository;

import com.example.paymentgateway.domain.entity.LedgerEntry;
import com.example.paymentgateway.repository.projection.WalletBalanceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
  @Query("select coalesce(sum(case when e.type = 'CREDIT' then e.amount else -e.amount end), 0) from LedgerEntry e where e.wallet.id = :walletId")
  BigDecimal calculateBalance(@Param("walletId") UUID walletId);

  List<LedgerEntry> findByReferenceId(UUID referenceId);

  @Query(value = """
    select e.wallet_id as walletId,
           coalesce(sum(case when e.type = 'CREDIT' then e.amount else -e.amount end), 0) as balance
      from ledger_entries e
     group by e.wallet_id
    """, nativeQuery = true)
  List<WalletBalanceProjection> aggregateBalances();
}
