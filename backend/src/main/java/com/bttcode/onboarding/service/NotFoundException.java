package com.bttcode.onboarding.service;

import java.util.UUID;

/**
 * The resource does not exist, or exists but belongs to someone else. Both cases answer 404 so an
 * applicant cannot probe which application ids exist.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, UUID id) {
        super(what + " " + id + " was not found.");
    }
}
