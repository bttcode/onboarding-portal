package com.bttcode.sanctionsmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * A fake external background-check service. It answers like a real provider (idempotent, with a
 * reference number) and can be told to fail, slow down or go down, to demonstrate how the
 * onboarding backend copes with an unreliable dependency.
 *
 * <p>Development and demo use only: it has no authentication.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SanctionsMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(SanctionsMockApplication.class, args);
    }
}
