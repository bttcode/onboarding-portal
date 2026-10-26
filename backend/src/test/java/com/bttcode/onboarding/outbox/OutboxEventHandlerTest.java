package com.bttcode.onboarding.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bttcode.onboarding.TestData;
import com.bttcode.onboarding.config.OnboardingProperties;
import com.bttcode.onboarding.domain.ApplicationStatus;
import com.bttcode.onboarding.domain.CheckStatus;
import com.bttcode.onboarding.domain.OnboardingApplication;
import com.bttcode.onboarding.domain.OutboxEvent;
import com.bttcode.onboarding.domain.OutboxStatus;
import com.bttcode.onboarding.repository.OnboardingApplicationRepository;
import com.bttcode.onboarding.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

@ExtendWith(MockitoExtension.class)
class OutboxEventHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");

    @Mock
    private OutboxEventRepository outbox;
    @Mock
    private OnboardingApplicationRepository applications;
    @Mock
    private SanctionsCheckClient sanctions;

    private OutboxEventHandler handler;
    private OnboardingApplication app;
    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        OnboardingProperties properties = new OnboardingProperties(2,
                new OnboardingProperties.Outbox(true, 2000, 10, 3, Duration.ofSeconds(5), Duration.ofMinutes(5)),
                null, null, null);
        handler = new OutboxEventHandler(outbox, applications, sanctions, properties, Clock.fixed(NOW, ZoneOffset.UTC));
        app = OnboardingApplication.submit("alice-sub", TestData.applicant("Muster"), NOW);
        event = OutboxEvent.sanctionsCheckRequested(app.getId(), NOW);
    }

    @Test
    void returnsFalseWhenNothingIsDue() {
        when(outbox.lockNextDue(NOW)).thenReturn(Optional.empty());

        assertThat(handler.processNext()).isFalse();
    }

    @Test
    void aSuccessfulCheckMovesTheApplicationToReview() {
        givenDueEvent();
        when(sanctions.check(eq(app.getId()), any()))
                .thenReturn(new SanctionsCheckClient.CheckResponse("CHK-42", "CLEAR", NOW));

        assertThat(handler.processNext()).isTrue();

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.IN_REVIEW);
        assertThat(app.getCheckReference()).isEqualTo("CHK-42");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DONE);
    }

    @Test
    void aFailedCallIsRetriedLater() {
        givenDueEvent();
        when(sanctions.check(eq(app.getId()), any()))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        handler.processNext();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(app.getCheckStatus()).isEqualTo(CheckStatus.PENDING);
    }

    @Test
    void afterTheLastAttemptTheCheckIsMarkedAsError() {
        givenDueEvent();
        when(sanctions.check(eq(app.getId()), any()))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        for (int attempt = 0; attempt < 3; attempt++) {
            handler.processNext();
        }

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(app.getCheckStatus()).isEqualTo(CheckStatus.ERROR);
    }

    @Test
    void aDuplicateEventDoesNotCallTheServiceAgain() {
        app.recordCheckResult(CheckStatus.CLEAR, "CHK-1", NOW);
        givenDueEvent();

        handler.processNext();

        verifyNoInteractions(sanctions);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DONE);
    }

    @Test
    void anUnexpectedResultIsTreatedAsAFailure() {
        givenDueEvent();
        when(sanctions.check(eq(app.getId()), any()))
                .thenReturn(new SanctionsCheckClient.CheckResponse("CHK-42", "MAYBE", NOW));

        handler.processNext();

        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(app.getCheckStatus()).isEqualTo(CheckStatus.PENDING);
    }

    private void givenDueEvent() {
        when(outbox.lockNextDue(NOW)).thenReturn(Optional.of(event));
        when(applications.findById(app.getId())).thenReturn(Optional.of(app));
    }
}
