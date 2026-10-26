package com.bttcode.onboarding.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bttcode.onboarding.TestData;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OnboardingApplicationTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");
    private static final int REQUIRED_APPROVALS = 2;

    @Test
    void newApplicationWaitsForTheBackgroundCheck() {
        OnboardingApplication app = OnboardingApplication.submit("alice-sub", TestData.applicant("Muster"), NOW);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(app.getCheckStatus()).isEqualTo(CheckStatus.PENDING);
    }

    @Test
    void checkResultMovesTheApplicationIntoReview() {
        OnboardingApplication app = submitted();

        app.recordCheckResult(CheckStatus.CLEAR, "CHK-1", NOW);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.IN_REVIEW);
        assertThat(app.getCheckReference()).isEqualTo("CHK-1");
    }

    @Test
    void aRedeliveredCheckResultChangesNothing() {
        OnboardingApplication app = submitted();
        app.recordCheckResult(CheckStatus.CLEAR, "CHK-1", NOW);

        app.recordCheckResult(CheckStatus.HIT, "CHK-2", NOW.plusSeconds(60));

        assertThat(app.getCheckStatus()).isEqualTo(CheckStatus.CLEAR);
        assertThat(app.getCheckReference()).isEqualTo("CHK-1");
    }

    @Test
    void approvalNeedsTwoDifferentCaseworkers() {
        OnboardingApplication app = inReview(CheckStatus.CLEAR);

        app.decide("carol", DecisionType.APPROVE, null, REQUIRED_APPROVALS, NOW);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.IN_REVIEW);

        app.decide("dave", DecisionType.APPROVE, null, REQUIRED_APPROVALS, NOW);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(app.approvalCount()).isEqualTo(2);
    }

    @Test
    void theSameCaseworkerCannotApproveTwice() {
        OnboardingApplication app = inReview(CheckStatus.CLEAR);
        app.decide("carol", DecisionType.APPROVE, null, REQUIRED_APPROVALS, NOW);

        assertThatThrownBy(() -> app.decide("carol", DecisionType.APPROVE, null, REQUIRED_APPROVALS, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).code())
                .isEqualTo("ALREADY_DECIDED");
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.IN_REVIEW);
    }

    @Test
    void oneRejectionIsFinal() {
        OnboardingApplication app = inReview(CheckStatus.CLEAR);

        app.decide("carol", DecisionType.REJECT, "Incomplete address", REQUIRED_APPROVALS, NOW);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThatThrownBy(() -> app.decide("dave", DecisionType.APPROVE, null, REQUIRED_APPROVALS, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("REJECTED");
    }

    @Test
    void approvingACheckHitNeedsAJustification() {
        OnboardingApplication app = inReview(CheckStatus.HIT);

        assertThatThrownBy(() -> app.decide("carol", DecisionType.APPROVE, "  ", REQUIRED_APPROVALS, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).code())
                .isEqualTo("JUSTIFICATION_REQUIRED");

        app.decide("carol", DecisionType.APPROVE, "Name match only, birth date differs", REQUIRED_APPROVALS, NOW);
        assertThat(app.approvalCount()).isEqualTo(1);
    }

    @Test
    void anApplicationCannotBeDecidedBeforeTheCheckIsDone() {
        OnboardingApplication app = submitted();

        assertThatThrownBy(() -> app.decide("carol", DecisionType.APPROVE, null, REQUIRED_APPROVALS, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).code())
                .isEqualTo("NOT_IN_REVIEW");
    }

    @Test
    void onlyAFailedCheckCanBeRetried() {
        OnboardingApplication app = submitted();
        assertThatThrownBy(app::requestRecheck).isInstanceOf(BusinessRuleException.class);

        app.markCheckFailed();
        app.requestRecheck();

        assertThat(app.getCheckStatus()).isEqualTo(CheckStatus.PENDING);
    }

    @Test
    void documentsAreFrozenOnceDecided() {
        OnboardingApplication app = inReview(CheckStatus.CLEAR);
        assertThat(app.acceptsDocuments()).isTrue();

        app.decide("carol", DecisionType.REJECT, null, REQUIRED_APPROVALS, NOW);

        assertThat(app.acceptsDocuments()).isFalse();
    }

    private static OnboardingApplication submitted() {
        return OnboardingApplication.submit("alice-sub", TestData.applicant("Muster"), NOW);
    }

    private static OnboardingApplication inReview(CheckStatus result) {
        OnboardingApplication app = submitted();
        app.recordCheckResult(result, "CHK-1", NOW);
        return app;
    }
}
