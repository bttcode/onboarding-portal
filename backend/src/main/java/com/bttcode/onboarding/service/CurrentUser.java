package com.bttcode.onboarding.service;

import java.util.Set;

/** The authenticated caller, taken from the JWT. */
public record CurrentUser(String subject, String username, Set<String> roles) {

    public static final String APPLICANT = "applicant";
    public static final String CASEWORKER = "caseworker";

    public boolean isCaseworker() {
        return roles.contains(CASEWORKER);
    }
}
