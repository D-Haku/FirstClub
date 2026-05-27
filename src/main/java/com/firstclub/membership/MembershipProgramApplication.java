package com.firstclub.membership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Boot entry point for the FirstClub Membership Program backend.
 *
 * <p>{@link EnableRetry} enables Spring Retry support so the
 * {@code SubscriptionService.changeTier} {@code @Retryable} annotation
 * (configured to retry on optimistic-lock failures) takes effect at
 * runtime.
 *
 * <p>Validates: Requirements 10.1, 10.2, 10.4.
 */
@SpringBootApplication
@EnableRetry
public class MembershipProgramApplication {

    public static void main(String[] args) {
        SpringApplication.run(MembershipProgramApplication.class, args);
    }
}
