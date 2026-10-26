package com.bttcode.onboarding.domain;

import java.time.LocalDate;

/** The data an applicant enters in the form, already trimmed and syntactically validated. */
public record ApplicantDetails(
        String firstName,
        String lastName,
        String email,
        LocalDate birthDate,
        String nationality,
        String street,
        String postalCode,
        String city,
        Product product,
        LocalDate startDate) {
}
