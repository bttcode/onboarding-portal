package com.bttcode.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bttcode.onboarding.TestData;
import com.bttcode.onboarding.config.OnboardingProperties;
import com.bttcode.onboarding.domain.ApplicantDetails;
import com.bttcode.onboarding.domain.BusinessRuleException;
import com.bttcode.onboarding.domain.CheckStatus;
import com.bttcode.onboarding.domain.DecisionType;
import com.bttcode.onboarding.domain.OnboardingApplication;
import com.bttcode.onboarding.domain.OutboxEvent;
import com.bttcode.onboarding.repository.ApplicationDocumentRepository;
import com.bttcode.onboarding.repository.OnboardingApplicationRepository;
import com.bttcode.onboarding.repository.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");
    private static final CurrentUser ALICE = new CurrentUser("alice-sub", "alice", Set.of(CurrentUser.APPLICANT));
    private static final CurrentUser CAROL = new CurrentUser("carol-sub", "carol", Set.of(CurrentUser.CASEWORKER));

    @Mock
    private OnboardingApplicationRepository applications;
    @Mock
    private ApplicationDocumentRepository documents;
    @Mock
    private OutboxEventRepository outbox;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        OnboardingProperties properties = new OnboardingProperties(2,
                new OnboardingProperties.Outbox(true, 2000, 10, 8, Duration.ofSeconds(5), Duration.ofMinutes(5)),
                new OnboardingProperties.Sanctions("http://localhost:8090", Duration.ofSeconds(2), Duration.ofSeconds(3)),
                new OnboardingProperties.Security("http://issuer", "http://issuer/certs", "onboarding-api",
                        List.of("http://localhost:4200")),
                new OnboardingProperties.Upload(5 * 1024 * 1024));
        service = new ApplicationService(applications, documents, outbox, new ApplicationMapper(), properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void submitStoresTheApplicationAndItsOutboxEventTogether() {
        var created = service.submit(ALICE, TestData.applicant("Muster"));

        ArgumentCaptor<OutboxEvent> event = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(applications).save(any(OnboardingApplication.class));
        verify(outbox).save(event.capture());
        assertThat(event.getValue().getAggregateId()).isEqualTo(created.getId());
        assertThat(event.getValue().getType()).isEqualTo(OutboxEvent.SANCTIONS_CHECK_REQUESTED);
    }

    @Test
    void submitRejectsMinors() {
        ApplicantDetails minor = withBirthDate(LocalDate.of(2010, 1, 1));

        assertThatThrownBy(() -> service.submit(ALICE, minor))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("18");
        verify(outbox, never()).save(any());
    }

    @Test
    void aCaseworkerCannotDecideTheirOwnApplication() {
        CurrentUser carolAsApplicantToo = new CurrentUser("carol-sub", "carol",
                Set.of(CurrentUser.APPLICANT, CurrentUser.CASEWORKER));
        OnboardingApplication own = OnboardingApplication.submit("carol-sub", TestData.applicant("Muster"), NOW);
        own.recordCheckResult(CheckStatus.CLEAR, "CHK-1", NOW);
        when(applications.findById(own.getId())).thenReturn(Optional.of(own));

        assertThatThrownBy(() -> service.decide(carolAsApplicantToo, own.getId(), DecisionType.APPROVE, null))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).code())
                .isEqualTo("OWN_APPLICATION");
    }

    @Test
    void anApplicantCannotSeeSomeoneElsesApplication() {
        OnboardingApplication bobs = OnboardingApplication.submit("bob-sub", TestData.applicant("Muster"), NOW);
        when(applications.findById(bobs.getId())).thenReturn(Optional.of(bobs));

        assertThatThrownBy(() -> service.get(ALICE, bobs.getId())).isInstanceOf(NotFoundException.class);
        assertThat(service.get(CAROL, bobs.getId()).getId()).isEqualTo(bobs.getId());
    }

    @Test
    void uploadRejectsAFileThatIsNotReallyAPdf() {
        OnboardingApplication app = OnboardingApplication.submit("alice-sub", TestData.applicant("Muster"), NOW);
        when(applications.findById(app.getId())).thenReturn(Optional.of(app));
        byte[] notAPdf = "MZ this is an executable".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> service.uploadDocument(ALICE, app.getId(), "id-card.pdf", notAPdf))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("PDF, PNG and JPEG");
        verify(documents, never()).save(any());
    }

    @Test
    void sortIsWhitelistedAndStable() {
        Sort sort = ApplicationService.parseSort("lastName,desc");

        assertThat(sort.getOrderFor("lastName")).isNotNull();
        assertThat(sort.getOrderFor("lastName").isDescending()).isTrue();
        assertThat(sort.getOrderFor("id")).isNotNull();
        assertThatThrownBy(() -> ApplicationService.parseSort("applicantSub,asc"))
                .isInstanceOf(InvalidRequestException.class);
    }

    private static ApplicantDetails withBirthDate(LocalDate birthDate) {
        ApplicantDetails d = TestData.applicant("Muster");
        return new ApplicantDetails(d.firstName(), d.lastName(), d.email(), birthDate, d.nationality(), d.street(),
                d.postalCode(), d.city(), d.product(), d.startDate());
    }
}
