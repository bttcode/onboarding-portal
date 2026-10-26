package com.bttcode.onboarding.outbox;

import com.bttcode.onboarding.config.OnboardingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Drains due outbox events on a fixed delay. Disabled in tests with {@code onboarding.outbox.enabled=false}. */
@Component
@ConditionalOnProperty(prefix = "onboarding.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxEventHandler handler;
    private final int batchSize;

    public OutboxPoller(OutboxEventHandler handler, OnboardingProperties properties) {
        this.handler = handler;
        this.batchSize = properties.outbox().batchSize();
    }

    @Scheduled(fixedDelayString = "${onboarding.outbox.poll-interval-ms:2000}")
    public void poll() {
        try {
            for (int i = 0; i < batchSize && handler.processNext(); i++) {
                // each call handles one event in its own transaction
            }
        } catch (RuntimeException e) {
            // a database hiccup must not kill the scheduler thread; the next tick tries again
            log.error("Outbox polling failed", e);
        }
    }
}
