package com.bttcode.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One caseworker's decision. The table has a unique key on (application, caseworker), so the
 * four-eyes rule also holds if two requests from the same caseworker race each other.
 */
@Entity
@Table(name = "application_decision")
public class ApplicationDecision {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private OnboardingApplication application;

    @Column(nullable = false, length = 100)
    private String caseworker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DecisionType decision;

    @Column(length = 1000)
    private String comment;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    protected ApplicationDecision() {
        // for JPA
    }

    ApplicationDecision(OnboardingApplication application, String caseworker, DecisionType decision,
                        String comment, Instant decidedAt) {
        this.id = UUID.randomUUID();
        this.application = application;
        this.caseworker = caseworker;
        this.decision = decision;
        this.comment = comment;
        this.decidedAt = decidedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCaseworker() {
        return caseworker;
    }

    public DecisionType getDecision() {
        return decision;
    }

    public String getComment() {
        return comment;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
