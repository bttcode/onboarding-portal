package com.bttcode.sanctionsmock;

import java.time.Instant;

/** {@code result} is CLEAR or HIT. */
public record CheckResponse(String reference, String result, Instant checkedAt) {
}
