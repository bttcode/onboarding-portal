package com.bttcode.onboarding;

import com.bttcode.onboarding.domain.ApplicantDetails;
import com.bttcode.onboarding.domain.Product;
import java.time.LocalDate;

public final class TestData {

    private TestData() {
    }

    public static ApplicantDetails applicant(String lastName) {
        return new ApplicantDetails("Anna", lastName, "anna@example.com", LocalDate.of(1990, 4, 12), "CH",
                "Bahnhofstrasse 1", "8001", "Zurich", Product.HOUSEHOLD, LocalDate.now().plusDays(30));
    }

    public static String applicantJson(String lastName) {
        return """
                {
                  "firstName": "Anna",
                  "lastName": "%s",
                  "email": "anna@example.com",
                  "birthDate": "1990-04-12",
                  "nationality": "CH",
                  "street": "Bahnhofstrasse 1",
                  "postalCode": "8001",
                  "city": "Zurich",
                  "product": "HOUSEHOLD",
                  "startDate": "%s"
                }
                """.formatted(lastName, LocalDate.now().plusDays(30));
    }
}
