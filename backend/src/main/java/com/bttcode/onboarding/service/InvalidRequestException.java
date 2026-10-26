package com.bttcode.onboarding.service;

/** Input that passes the schema but fails a semantic check (age, dates, file type). Mapped to 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
