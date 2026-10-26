package com.bttcode.sanctionsmock;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Failure injection switches.
 *
 * @param down        answer every check with 503
 * @param failureRate share of checks (0.0 to 1.0) answered with 503
 * @param latencyMs   delay added before every answer, to trigger the caller's read timeout
 */
public record ChaosSettings(
        boolean down,
        @DecimalMin("0.0") @DecimalMax("1.0") double failureRate,
        @Min(0) @Max(30_000) long latencyMs) {

    public static final ChaosSettings HEALTHY = new ChaosSettings(false, 0.0, 0);
}
