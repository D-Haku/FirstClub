package com.firstclub.membership.tier;

import com.firstclub.membership.domain.MembershipTier;

import java.time.Instant;

/**
 * In-memory cache value used by {@link TierRecommendationService}.
 *
 * <p>Couples a previously computed {@link MembershipTier} with the instant
 * at which the entry should be considered stale, supporting the TTL-bounded
 * cache described in the design.
 *
 * <p>Validates: Requirements 6.1, 6.3.
 *
 * @param tier      the cached recommended tier
 * @param expiresAt instant at which this cache entry is no longer fresh
 */
public record CachedRecommendation(MembershipTier tier, Instant expiresAt) {
}
