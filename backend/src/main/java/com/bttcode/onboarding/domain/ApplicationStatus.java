package com.bttcode.onboarding.domain;

public enum ApplicationStatus {
    /** Submitted, waiting for the background check. */
    SUBMITTED,
    /** Background check done, waiting for caseworker decisions. */
    IN_REVIEW,
    APPROVED,
    REJECTED
}
