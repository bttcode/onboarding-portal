package com.bttcode.onboarding.outbox;

import com.bttcode.onboarding.config.OnboardingProperties;
import com.bttcode.onboarding.domain.CheckStatus;
import com.bttcode.onboarding.domain.OnboardingApplication;
import com.bttcode.onboarding.domain.OutboxEvent;
import com.bttcode.onboarding.domain.OutboxStatus;
import com.bttcode.onboarding.repository.OnboardingApplicationRepository;
import com.bttcode.onboarding.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes one outbox event per transaction. The event row stays locked for the whole
 * transaction, so no other poller can pick it up while the external call is in flight.
 */
@Component
public class OutboxEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventHandler.class);

    private final OutboxEventRepository outbox;
    private final OnboardingApplicationRepository applications;
    private final SanctionsCheckClient sanctions;
    private final OnboardingProperties.Outbox settings;
    private final Clock clock;

    public OutboxEventHandler(OutboxEventRepository outbox, OnboardingApplicationRepository applications,
                              SanctionsCheckClient sanctions, OnboardingProperties properties, Clock clock) {
        this.outbox = outbox;
        this.applications = applications;
        this.sanctions = sanctions;
        this.settings = properties.outbox();
        this.clock = clock;
    }

    /** @return {@code true} if an event was claimed, {@code false} if nothing was due */
    @Transactional
    public boolean processNext() {
        Optional<OutboxEvent> next = outbox.lockNextDue(clock.instant());
        if (next.isEmpty()) {
            return false;
        }
        OutboxEvent event = next.get();
        if (!OutboxEvent.SANCTIONS_CHECK_REQUESTED.equals(event.getType())) {
            log.error("Unknown outbox event type {} for event {}", event.getType(), event.getId());
            event.recordFailure("Unknown event type " + event.getType(), clock.instant(), 1,
                    settings.baseBackoff(), settings.maxBackoff());
            return true;
        }
        handleSanctionsCheck(event);
        return true;
    }

    private void handleSanctionsCheck(OutboxEvent event) {
        Instant now = clock.instant();
        OnboardingApplication app = applications.findById(event.getAggregateId()).orElse(null);
        if (app == null || app.getCheckStatus() != CheckStatus.PENDING) {
            // already handled (e.g. a duplicate event): nothing to call, just close it
            event.markDone(now);
            return;
        }
        try {
            SanctionsCheckClient.CheckResponse response = sanctions.check(app.getId(), new SanctionsCheckClient.CheckRequest(
                    app.getFirstName(), app.getLastName(), app.getBirthDate(), app.getNationality()));
            CheckStatus result = CheckStatus.valueOf(response.result());
            app.recordCheckResult(result, response.reference(), response.checkedAt() != null ? response.checkedAt() : now);
            event.markDone(now);
            log.info("Background check for application {}: {}", app.getId(), result);
        } catch (RuntimeException e) {
            event.recordFailure(e.getClass().getSimpleName() + ": " + e.getMessage(), now,
                    settings.maxAttempts(), settings.baseBackoff(), settings.maxBackoff());
            if (event.getStatus() == OutboxStatus.FAILED) {
                app.markCheckFailed();
                log.error("Background check for application {} failed {} times; giving up",
                        app.getId(), event.getAttempts(), e);
            } else {
                log.warn("Background check for application {} failed (attempt {}), next try at {}: {}",
                        app.getId(), event.getAttempts(), event.getNextAttemptAt(), e.toString());
            }
        }
    }
}
