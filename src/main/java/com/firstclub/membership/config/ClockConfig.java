package com.firstclub.membership.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the application-wide {@link Clock} bean.
 *
 * <p>All services that reason about time (subscription periods, tier
 * recommendation TTL, idempotency record timestamps) inject {@link Clock}
 * rather than calling {@code Instant.now()} directly so tests can swap in
 * a {@link Clock#fixed} or stepped clock without modifying production
 * code.
 */
@Configuration
public class ClockConfig {

    /**
     * @return the system UTC clock used by the application
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
