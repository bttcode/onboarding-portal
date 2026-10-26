package com.bttcode.onboarding.repository;

import com.bttcode.onboarding.domain.OutboxEvent;
import com.bttcode.onboarding.domain.OutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Claims the oldest due event and keeps it row-locked until the caller's transaction ends.
     * {@code SKIP LOCKED} lets several backend instances poll the same table without ever
     * handling the same event twice and without waiting on each other.
     */
    @Query(value = """
            select * from outbox_event
            where status = 'PENDING' and next_attempt_at <= :now
            order by created_at
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<OutboxEvent> lockNextDue(@Param("now") Instant now);

    List<OutboxEvent> findByAggregateIdOrderByCreatedAt(UUID aggregateId);

    long countByStatus(OutboxStatus status);
}
