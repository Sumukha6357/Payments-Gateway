package com.example.paymentgateway.repository;

import com.example.paymentgateway.domain.entity.OutboxEvent;
import com.example.paymentgateway.domain.enumtype.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
  @Query("select e from OutboxEvent e where e.status in :statuses and e.nextAttemptAt <= :now order by e.createdAt asc")
  List<OutboxEvent> findDispatchable(@Param("statuses") List<OutboxStatus> statuses, @Param("now") OffsetDateTime now);

  long countByStatus(OutboxStatus status);
}
