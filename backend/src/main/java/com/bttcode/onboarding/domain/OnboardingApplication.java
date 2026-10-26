package com.bttcode.onboarding.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * An onboarding application and its review lifecycle.
 *
 * <pre>
 * SUBMITTED --check CLEAR/HIT--> IN_REVIEW --N approvals--> APPROVED
 *                                          \--1 rejection--> REJECTED
 * </pre>
 *
 * All state changes go through methods on this class, so the business rules live in one place
 * and can be tested without Spring or a database.
 */
@Entity
@Table(name = "onboarding_application")
public class OnboardingApplication {

    @Id
    private UUID id;

    /** Optimistic locking: two caseworkers deciding at the same moment cannot both win. */
    @Version
    private Long version;

    @Column(name = "applicant_sub", nullable = false, length = 64)
    private String applicantSub;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 2)
    private String nationality;

    @Column(nullable = false, length = 200)
    private String street;

    @Column(name = "postal_code", nullable = false, length = 4)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Product product;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_status", nullable = false, length = 16)
    private CheckStatus checkStatus;

    @Column(name = "check_reference", length = 64)
    private String checkReference;

    @Column(name = "checked_at")
    private Instant checkedAt;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("decidedAt ASC")
    private List<ApplicationDecision> decisions = new ArrayList<>();

    protected OnboardingApplication() {
        // for JPA
    }

    public static OnboardingApplication submit(String applicantSub, ApplicantDetails details, Instant now) {
        OnboardingApplication app = new OnboardingApplication();
        app.id = UUID.randomUUID();
        app.applicantSub = applicantSub;
        app.firstName = details.firstName();
        app.lastName = details.lastName();
        app.email = details.email();
        app.birthDate = details.birthDate();
        app.nationality = details.nationality();
        app.street = details.street();
        app.postalCode = details.postalCode();
        app.city = details.city();
        app.product = details.product();
        app.startDate = details.startDate();
        app.status = ApplicationStatus.SUBMITTED;
        app.checkStatus = CheckStatus.PENDING;
        app.submittedAt = now;
        return app;
    }

    /**
     * Stores the background-check result. A second delivery of the same result (the outbox
     * guarantees at-least-once) changes nothing.
     */
    public void recordCheckResult(CheckStatus result, String reference, Instant checkedAt) {
        if (result != CheckStatus.CLEAR && result != CheckStatus.HIT) {
            throw new IllegalArgumentException("A check result must be CLEAR or HIT, was " + result);
        }
        if (checkStatus != CheckStatus.PENDING) {
            return;
        }
        this.checkStatus = result;
        this.checkReference = reference;
        this.checkedAt = checkedAt;
        if (status == ApplicationStatus.SUBMITTED) {
            status = ApplicationStatus.IN_REVIEW;
        }
    }

    public void markCheckFailed() {
        if (checkStatus == CheckStatus.PENDING) {
            checkStatus = CheckStatus.ERROR;
        }
    }

    public void requestRecheck() {
        if (checkStatus != CheckStatus.ERROR) {
            throw new BusinessRuleException("CHECK_NOT_FAILED", "Only a failed background check can be retried.");
        }
        checkStatus = CheckStatus.PENDING;
    }

    /**
     * Records a caseworker decision under the four-eyes principle: approval needs
     * {@code requiredApprovals} different caseworkers, and one rejection is final.
     */
    public void decide(String caseworker, DecisionType type, String comment, int requiredApprovals, Instant now) {
        if (status != ApplicationStatus.IN_REVIEW) {
            throw new BusinessRuleException("NOT_IN_REVIEW",
                    "Only applications in review can be decided; this one is " + status + ".");
        }
        if (decisions.stream().anyMatch(d -> d.getCaseworker().equals(caseworker))) {
            throw new BusinessRuleException("ALREADY_DECIDED",
                    "You already decided on this application; a second caseworker must review it.");
        }
        boolean hasComment = comment != null && !comment.isBlank();
        if (type == DecisionType.APPROVE && checkStatus == CheckStatus.HIT && !hasComment) {
            throw new BusinessRuleException("JUSTIFICATION_REQUIRED",
                    "The background check found a match. Approving needs a justification comment.");
        }
        decisions.add(new ApplicationDecision(this, caseworker, type, hasComment ? comment.strip() : null, now));
        if (type == DecisionType.REJECT) {
            status = ApplicationStatus.REJECTED;
        } else if (approvalCount() >= requiredApprovals) {
            status = ApplicationStatus.APPROVED;
        }
    }

    public boolean acceptsDocuments() {
        return status == ApplicationStatus.SUBMITTED || status == ApplicationStatus.IN_REVIEW;
    }

    public void attachDocument(String fileName) {
        this.documentName = fileName;
    }

    public int approvalCount() {
        return (int) decisions.stream().filter(d -> d.getDecision() == DecisionType.APPROVE).count();
    }

    public boolean isOwnedBy(String subject) {
        return applicantSub.equals(subject);
    }

    public UUID getId() {
        return id;
    }

    public String getApplicantSub() {
        return applicantSub;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getNationality() {
        return nationality;
    }

    public String getStreet() {
        return street;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public Product getProduct() {
        return product;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public CheckStatus getCheckStatus() {
        return checkStatus;
    }

    public String getCheckReference() {
        return checkReference;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public String getDocumentName() {
        return documentName;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public List<ApplicationDecision> getDecisions() {
        return Collections.unmodifiableList(decisions);
    }
}
