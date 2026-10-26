package com.bttcode.sanctionsmock;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CheckService {

    private final ConcurrentMap<String, CheckResponse> resultsByKey = new ConcurrentHashMap<>();
    private final AtomicReference<ChaosSettings> chaos = new AtomicReference<>(ChaosSettings.HEALTHY);
    private final AtomicLong calls = new AtomicLong();
    private final AtomicLong failedCalls = new AtomicLong();
    private final Set<String> listedLastNames;

    public CheckService(MockProperties properties) {
        this.listedLastNames = properties.listedLastNames().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @return the result, or {@code null} if chaos settings say this call fails
     */
    public CheckResponse check(String idempotencyKey, CheckRequest request) throws InterruptedException {
        calls.incrementAndGet();
        ChaosSettings settings = chaos.get();
        if (settings.latencyMs() > 0) {
            Thread.sleep(settings.latencyMs());
        }
        if (settings.down() || ThreadLocalRandom.current().nextDouble() < settings.failureRate()) {
            failedCalls.incrementAndGet();
            return null;
        }
        // same key, same answer: a retried call never runs a second check
        return resultsByKey.computeIfAbsent(idempotencyKey, key -> evaluate(request));
    }

    private CheckResponse evaluate(CheckRequest request) {
        boolean listed = listedLastNames.contains(request.lastName().strip().toLowerCase(Locale.ROOT));
        String reference = "CHK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return new CheckResponse(reference, listed ? "HIT" : "CLEAR", Instant.now());
    }

    public ChaosSettings chaos() {
        return chaos.get();
    }

    public void setChaos(ChaosSettings settings) {
        chaos.set(settings);
    }

    public Stats stats() {
        return new Stats(calls.get(), failedCalls.get(), resultsByKey.size());
    }

    /**
     * @param totalCalls     every request received, including retries
     * @param failedCalls    requests answered with 503 by the chaos settings
     * @param distinctChecks checks actually performed (one per idempotency key)
     */
    public record Stats(long totalCalls, long failedCalls, int distinctChecks) {
    }
}
