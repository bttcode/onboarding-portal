package com.bttcode.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox row. It is written in the same database transaction as the change that
 * needs a side effect, so the side effect can never be lost, even if the process dies right after
 * the commit. A poller performs the side effect later and records the outcome here.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    public static final String SANCTIONS_CHECK_REQUESTED = "SANCTIONS_CHECK_REQUESTED";

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 64)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected OutboxEvent() {
        // for JPA
    }

    public static OutboxEvent sanctionsCheckRequested(UUID applicationId, Instant now) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.aggregateId = applicationId;
        event.type = SANCTIONS_CHECK_REQUESTED;
        event.status = OutboxStatus.PENDING;
        event.attempts = 0;
        event.nextAttemptAt = now;
        event.createdAt = now;
        return event;
    }

    public void markDone(Instant now) {
        status = OutboxStatus.DONE;
        processedAt = now;
    }

    /**
     * Records a failed attempt. The next attempt is delayed exponentially
     * ({@code base * 2^(attempts-1)}, capped at {@code maxBackoff}); after {@code maxAttempts}
     * the event is marked FAILED and left for a human.
     */
    public void recordFailure(String error, Instant now, int maxAttempts, Duration baseBackoff, Duration maxBackoff) {
        attempts++;
        lastError = truncate(error);
        if (attempts >= maxAttempts) {
            status = OutboxStatus.FAILED;
            processedAt = now;
            return;
        }
        nextAttemptAt = now.plus(backoff(attempts, baseBackoff, maxBackoff));
    }

    static Duration backoff(int attempts, Duration base, Duration max) {
        int exponent = Math.min(attempts - 1, 20);
        Duration delay = base.multipliedBy(1L << exponent);
        return delay.compareTo(max) > 0 ? max : delay;
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getType() {
        return type;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
