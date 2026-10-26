package com.bttcode.onboarding;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class OnboardingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnboardingBackendApplication.class, args);
    }

    /** Injected wherever "now" matters, so tests can pin time with a fixed clock. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
