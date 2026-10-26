package com.bttcode.onboarding.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");
    private static final Duration BASE = Duration.ofSeconds(5);
    private static final Duration MAX = Duration.ofMinutes(5);

    @Test
    void backoffDoublesAndIsCapped() {
        assertThat(OutboxEvent.backoff(1, BASE, MAX)).isEqualTo(Duration.ofSeconds(5));
        assertThat(OutboxEvent.backoff(2, BASE, MAX)).isEqualTo(Duration.ofSeconds(10));
        assertThat(OutboxEvent.backoff(4, BASE, MAX)).isEqualTo(Duration.ofSeconds(40));
        assertThat(OutboxEvent.backoff(10, BASE, MAX)).isEqualTo(MAX);
        assertThat(OutboxEvent.backoff(500, BASE, MAX)).isEqualTo(MAX);
    }

    @Test
    void aFailureSchedulesTheNextAttempt() {
        OutboxEvent event = OutboxEvent.sanctionsCheckRequested(UUID.randomUUID(), NOW);

        event.recordFailure("503 Service Unavailable", NOW, 8, BASE, MAX);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(event.getLastError()).isEqualTo("503 Service Unavailable");
    }

    @Test
    void theEventFailsAfterTheLastAttempt() {
        OutboxEvent event = OutboxEvent.sanctionsCheckRequested(UUID.randomUUID(), NOW);

        for (int i = 0; i < 3; i++) {
            event.recordFailure("timeout", NOW, 3, BASE, MAX);
        }

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getProcessedAt()).isEqualTo(NOW);
    }

    @Test
    void longErrorsAreTruncatedToFitTheColumn() {
        OutboxEvent event = OutboxEvent.sanctionsCheckRequested(UUID.randomUUID(), NOW);

        event.recordFailure("x".repeat(2000), NOW, 8, BASE, MAX);

        assertThat(event.getLastError()).hasSize(500);
    }
}
