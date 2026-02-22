package com.example.paymentgateway.repository;

import com.example.paymentgateway.domain.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
  Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

  @Override
  @EntityGraph(attributePaths = {"fromWallet", "toWallet"})
  Page<Transaction> findAll(Pageable pageable);
}
