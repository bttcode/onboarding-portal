package com.bttcode.onboarding;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Runs against a real PostgreSQL in Docker, so the Liquibase migrations, the native
 * {@code FOR UPDATE SKIP LOCKED} query and the constraints are tested for real.
 *
 * <p>One container is shared by all integration test classes (singleton pattern): Spring caches
 * the application context between classes, so the database must outlive any single class.
 */
@SpringBootTest(properties = "onboarding.outbox.enabled=false")
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected static RequestPostProcessor applicant(String username) {
        return user(username, "applicant");
    }

    protected static RequestPostProcessor caseworker(String username) {
        return user(username, "caseworker");
    }

    private static RequestPostProcessor user(String username, String role) {
        return jwt()
                .jwt(token -> token.subject(username + "-sub").claim("preferred_username", username))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
