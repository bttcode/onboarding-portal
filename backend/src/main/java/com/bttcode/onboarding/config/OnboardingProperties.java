package com.bttcode.onboarding.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Every tunable of the onboarding backend, bound from the {@code onboarding.*} keys. */
@Validated
@ConfigurationProperties(prefix = "onboarding")
public record OnboardingProperties(
        @DefaultValue("2") @Min(1) int requiredApprovals,
        @Valid @DefaultValue Outbox outbox,
        @Valid @DefaultValue Sanctions sanctions,
        @Valid Security security,
        @Valid @DefaultValue Upload upload) {

    public record Outbox(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("2000") @Min(100) long pollIntervalMs,
            @DefaultValue("10") @Min(1) int batchSize,
            @DefaultValue("8") @Min(1) int maxAttempts,
            @DefaultValue("5s") Duration baseBackoff,
            @DefaultValue("5m") Duration maxBackoff) {
    }

    public record Sanctions(
            @DefaultValue("http://localhost:8090") @NotBlank String baseUrl,
            @DefaultValue("2s") Duration connectTimeout,
            @DefaultValue("3s") Duration readTimeout) {
    }

    public record Security(
            @NotBlank String issuerUri,
            @NotBlank String jwkSetUri,
            @DefaultValue("onboarding-api") @NotBlank String audience,
            @DefaultValue("http://localhost:4200") @NotEmpty List<String> allowedOrigins) {
    }

    public record Upload(@DefaultValue("5242880") @Min(1) long maxBytes) {
    }
}
