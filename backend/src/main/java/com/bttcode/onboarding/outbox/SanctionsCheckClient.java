package com.bttcode.onboarding.outbox;

import com.bttcode.onboarding.config.OnboardingProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.http.HttpClient;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the external background-check service.
 *
 * <p>Resilience4j applies {@code Retry(CircuitBreaker(call))}: short outages are absorbed by a
 * few quick retries, and while the service is clearly down the open circuit fails calls at once
 * instead of tying up the poller with timeouts. The outbox adds the slow, long-term retry on top.
 *
 * <p>The application id is sent as {@code Idempotency-Key}, so a retried call after a lost
 * response returns the original result instead of running a second check.
 */
@Component
public class SanctionsCheckClient {

    private final RestClient restClient;

    public SanctionsCheckClient(RestClient.Builder builder, OnboardingProperties properties) {
        OnboardingProperties.Sanctions settings = properties.sanctions();
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(settings.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(settings.readTimeout());
        this.restClient = builder.baseUrl(settings.baseUrl()).requestFactory(requestFactory).build();
    }

    @Retry(name = "sanctions")
    @CircuitBreaker(name = "sanctions")
    public CheckResponse check(UUID applicationId, CheckRequest request) {
        CheckResponse response = restClient.post()
                .uri("/checks")
                .header("Idempotency-Key", applicationId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CheckResponse.class);
        if (response == null || response.result() == null) {
            throw new IllegalStateException("The check service returned an empty response");
        }
        return response;
    }

    public record CheckRequest(String firstName, String lastName, LocalDate birthDate, String nationality) {
    }

    /** {@code result} is CLEAR or HIT. */
    public record CheckResponse(String reference, String result, Instant checkedAt) {
    }
}
