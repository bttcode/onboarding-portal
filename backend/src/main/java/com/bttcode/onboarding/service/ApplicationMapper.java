package com.bttcode.onboarding.service;

import com.bttcode.onboarding.api.model.ApplicantData;
import com.bttcode.onboarding.api.model.ApplicationDetail;
import com.bttcode.onboarding.api.model.ApplicationPage;
import com.bttcode.onboarding.api.model.ApplicationStatus;
import com.bttcode.onboarding.api.model.ApplicationSummary;
import com.bttcode.onboarding.api.model.CheckResult;
import com.bttcode.onboarding.api.model.CheckStatus;
import com.bttcode.onboarding.api.model.Decision;
import com.bttcode.onboarding.api.model.DecisionType;
import com.bttcode.onboarding.api.model.Product;
import com.bttcode.onboarding.domain.ApplicantDetails;
import com.bttcode.onboarding.domain.ApplicationDecision;
import com.bttcode.onboarding.domain.OnboardingApplication;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Converts between the generated API models and the domain. Enum types share constant names on
 * both sides, so they convert by name; a renamed constant fails fast here instead of silently.
 */
@Component
public class ApplicationMapper {

    public ApplicantDetails toDetails(ApplicantData data) {
        return new ApplicantDetails(
                data.getFirstName().strip(),
                data.getLastName().strip(),
                data.getEmail().strip().toLowerCase(),
                data.getBirthDate(),
                data.getNationality(),
                data.getStreet().strip(),
                data.getPostalCode(),
                data.getCity().strip(),
                convert(data.getProduct(), com.bttcode.onboarding.domain.Product.class),
                data.getStartDate());
    }

    public com.bttcode.onboarding.domain.DecisionType toDomain(DecisionType type) {
        return convert(type, com.bttcode.onboarding.domain.DecisionType.class);
    }

    public com.bttcode.onboarding.domain.ApplicationStatus toDomain(ApplicationStatus status) {
        return status == null ? null : convert(status, com.bttcode.onboarding.domain.ApplicationStatus.class);
    }

    public ApplicationSummary toSummary(OnboardingApplication app) {
        return new ApplicationSummary()
                .id(app.getId())
                .applicantName(app.getFirstName() + " " + app.getLastName())
                .product(convert(app.getProduct(), Product.class))
                .status(convert(app.getStatus(), ApplicationStatus.class))
                .checkStatus(convert(app.getCheckStatus(), CheckStatus.class))
                .submittedAt(utc(app.getSubmittedAt()))
                .approvals(app.approvalCount())
                .hasDocument(app.getDocumentName() != null);
    }

    public ApplicationDetail toDetail(OnboardingApplication app, int requiredApprovals) {
        return new ApplicationDetail()
                .id(app.getId())
                .status(convert(app.getStatus(), ApplicationStatus.class))
                .submittedAt(utc(app.getSubmittedAt()))
                .applicant(toApplicantData(app))
                .check(new CheckResult()
                        .status(convert(app.getCheckStatus(), CheckStatus.class))
                        .reference(app.getCheckReference())
                        .checkedAt(utc(app.getCheckedAt())))
                .decisions(app.getDecisions().stream().map(this::toDecision).toList())
                .requiredApprovals(requiredApprovals)
                .documentName(app.getDocumentName());
    }

    public ApplicationPage toPage(Page<OnboardingApplication> page) {
        return new ApplicationPage()
                .content(page.getContent().stream().map(this::toSummary).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages());
    }

    private ApplicantData toApplicantData(OnboardingApplication app) {
        return new ApplicantData()
                .firstName(app.getFirstName())
                .lastName(app.getLastName())
                .email(app.getEmail())
                .birthDate(app.getBirthDate())
                .nationality(app.getNationality())
                .street(app.getStreet())
                .postalCode(app.getPostalCode())
                .city(app.getCity())
                .product(convert(app.getProduct(), Product.class))
                .startDate(app.getStartDate());
    }

    private Decision toDecision(ApplicationDecision decision) {
        return new Decision()
                .caseworker(decision.getCaseworker())
                .decision(convert(decision.getDecision(), DecisionType.class))
                .comment(decision.getComment())
                .decidedAt(utc(decision.getDecidedAt()));
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static <E extends Enum<E>> E convert(Enum<?> source, Class<E> target) {
        return Enum.valueOf(target, source.name());
    }
}
