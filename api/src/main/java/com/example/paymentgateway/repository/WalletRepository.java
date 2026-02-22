package com.example.paymentgateway.repository;

import com.example.paymentgateway.domain.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select w from Wallet w where w.id = :id")
  Optional<Wallet> findByIdForUpdate(@Param("id") UUID id);

  @Override
  @EntityGraph(attributePaths = {"user"})
  Page<Wallet> findAll(Pageable pageable);
}
