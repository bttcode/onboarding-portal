package com.bttcode.onboarding.domain;

public enum OutboxStatus {
    PENDING,
    DONE,
    /** Gave up after the maximum number of attempts. */
    FAILED
}
