package com.bttcode.onboarding.domain;

public enum CheckStatus {
    PENDING,
    CLEAR,
    HIT,
    /** The check service kept failing until the outbox gave up; a caseworker can retry. */
    ERROR
}
