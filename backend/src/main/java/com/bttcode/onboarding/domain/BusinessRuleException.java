package com.bttcode.onboarding.domain;

/** A request that is well-formed but breaks a business rule. Mapped to HTTP 409 with {@link #code()}. */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
