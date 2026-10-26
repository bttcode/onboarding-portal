package com.bttcode.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bttcode.onboarding.domain.ApplicationStatus;
import com.bttcode.onboarding.domain.OnboardingApplication;
import com.bttcode.onboarding.domain.OutboxEvent;
import com.bttcode.onboarding.domain.OutboxStatus;
import com.bttcode.onboarding.outbox.OutboxEventHandler;
import com.bttcode.onboarding.outbox.SanctionsCheckClient;
import com.bttcode.onboarding.repository.OnboardingApplicationRepository;
import com.bttcode.onboarding.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxProcessingIT extends IntegrationTestBase {

    @MockBean
    private SanctionsCheckClient sanctions;

    @Autowired
    private OutboxEventHandler handler;
    @Autowired
    private OutboxEventRepository outbox;
    @Autowired
    private OnboardingApplicationRepository applications;
    @Autowired
    private TransactionTemplate tx;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @BeforeEach
    void startWithAnEmptyOutbox() {
        // other test classes leave pending events behind (their poller is disabled)
        outbox.deleteAll();
    }

    @AfterEach
    void stopExecutor() {
        executor.shutdownNow();
    }

    @Test
    void aPendingCheckIsProcessedEndToEnd() {
        UUID id = persistSubmittedApplication();
        when(sanctions.check(eq(id), any())).thenReturn(new SanctionsCheckClient.CheckResponse("CHK-7", "HIT", Instant.now()));

        assertThat(handler.processNext()).isTrue();
        assertThat(handler.processNext()).isFalse();

        OnboardingApplication app = applications.findById(id).orElseThrow();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.IN_REVIEW);
        assertThat(app.getCheckReference()).isEqualTo("CHK-7");
        assertThat(outbox.findByAggregateIdOrderByCreatedAt(id))
                .singleElement()
                .extracting(OutboxEvent::getStatus)
                .isEqualTo(OutboxStatus.DONE);
    }

    @Test
    void anEventLockedByOnePollerIsSkippedByAnother() throws Exception {
        persistSubmittedApplication();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<?> firstPoller = executor.submit(() -> tx.executeWithoutResult(status -> {
            assertThat(outbox.lockNextDue(Instant.now())).isPresent();
            locked.countDown();
            awaitQuietly(release);
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        Optional<OutboxEvent> secondPoller = tx.execute(status -> outbox.lockNextDue(Instant.now()));

        release.countDown();
        firstPoller.get(10, TimeUnit.SECONDS);
        assertThat(secondPoller).isEmpty();
    }

    private UUID persistSubmittedApplication() {
        return tx.execute(status -> {
            OnboardingApplication app = OnboardingApplication.submit("alice-sub", TestData.applicant("Outbox"), Instant.now());
            applications.save(app);
            outbox.save(OutboxEvent.sanctionsCheckRequested(app.getId(), Instant.now().minusSeconds(1)));
            return app.getId();
        });
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
