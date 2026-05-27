package com.firstclub.membership.tier;

import com.firstclub.membership.domain.User;

import java.time.Clock;

/**
 * Strategy interface for a single tier-recommendation signal.
 *
 * <p>Each implementation evaluates one signal (e.g., trailing-window order
 * count, monthly spend, cohort membership) and returns a {@link TierScore}
 * suggesting a {@link com.firstclub.membership.domain.MembershipTier} with
 * an integer strength. The {@link TierRecommendationService} aggregates the
 * scores across all configured criteria by summing
 * {@code strength * weight()} per suggested tier and breaking ties
 * deterministically.
 *
 * <p>The {@link Clock} is passed in explicitly so criteria stay pure with
 * respect to wall-clock time, which keeps the service deterministic and
 * testable.
 *
 * <p>Validates: Requirements 6.1, 6.3.
 */
public interface TierCriterion {

    /**
     * @return a stable identifier for this criterion, used as the secondary
     *         deterministic tie-breaker in score aggregation.
     */
    String id();

    /**
     * @return the configured weight applied to this criterion's score during
     *         aggregation; must be non-negative.
     */
    int weight();

    /**
     * Evaluate this criterion for the given user.
     *
     * @param user  the user whose recommendation is being computed
     * @param clock clock used for any time-window calculations
     * @return the criterion's vote as a {@link TierScore}; never {@code null}
     */
    TierScore evaluate(User user, Clock clock);
}
