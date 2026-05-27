package com.firstclub.membership.tier;

import com.firstclub.membership.domain.MembershipTier;

/**
 * Result of a single {@link TierCriterion} evaluation.
 *
 * <p>A criterion votes for a {@link MembershipTier} suggestion with a numeric
 * {@code strength}. The {@link TierRecommendationService} aggregates scores
 * across all criteria by summing {@code strength} times the criterion's
 * configured weight per suggested tier.
 *
 * <p>Validates: Requirements 6.1, 6.3.
 *
 * @param suggestion the tier this criterion votes for
 * @param strength   the magnitude of the vote (non-negative integer)
 */
public record TierScore(MembershipTier suggestion, int strength) {
}
