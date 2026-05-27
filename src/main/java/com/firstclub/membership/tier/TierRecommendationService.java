package com.firstclub.membership.tier;

import com.firstclub.membership.domain.MembershipTier;
import com.firstclub.membership.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates {@link TierCriterion} votes into a single {@link MembershipTier}
 * recommendation per {@link User}, with a TTL-bounded in-memory cache.
 *
 * <p>The service is the application-side consumer of every registered
 * {@link TierCriterion}. For each {@code recommend(user)} call it either
 * returns a fresh cached entry or recomputes the tier by summing
 * {@code strength * weight} per suggested tier and breaking ties
 * deterministically (higher tier ordinal wins inside a tie). Concurrent
 * fillers for the same user collapse onto a single computation thanks to
 * {@link ConcurrentHashMap#compute(Object, java.util.function.BiFunction)}.
 *
 * <p>Per Requirement 6.6, recommendation must never block other membership
 * operations. Any {@link RuntimeException} raised by a criterion is logged
 * and swallowed; the service falls back to {@link MembershipTier#SILVER}
 * so callers always receive a response.
 *
 * <p>Validates: Requirements 6.1, 6.2, 6.4, 6.5, 6.6, 9.5.
 */
@Service
public class TierRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(TierRecommendationService.class);

    private final TierCriterionRegistry registry;
    private final Clock clock;
    private final Duration ttl;
    private final ConcurrentHashMap<String, CachedRecommendation> cache = new ConcurrentHashMap<>();

    /**
     * Constructs the service with its collaborators.
     *
     * @param registry registry of all available {@link TierCriterion} beans
     * @param clock    clock used both to determine cache freshness and to feed
     *                 deterministic time-window calculations inside criteria
     * @param ttl      cache freshness window; configurable via
     *                 {@code membership.recommendation.ttl} (default {@code PT5M})
     */
    public TierRecommendationService(
            TierCriterionRegistry registry,
            Clock clock,
            @Value("${membership.recommendation.ttl:PT5M}") Duration ttl) {
        this.registry = registry;
        this.clock = clock;
        this.ttl = ttl;
    }

    /**
     * Recommend a tier for the given user, consulting the cache first.
     *
     * <p>Concurrent callers for the same user share a single computation
     * because {@link ConcurrentHashMap#compute(Object, java.util.function.BiFunction)}
     * holds the bin lock during the recompute.
     *
     * @param user the user to recommend for; must not be {@code null}
     * @return the recommended {@link MembershipTier}; never {@code null}
     */
    public MembershipTier recommend(User user) {
        CachedRecommendation cached = cache.compute(user.getId(), (key, existing) -> {
            if (existing != null && existing.expiresAt().isAfter(clock.instant())) {
                return existing;
            }
            MembershipTier fresh = aggregate(user);
            return new CachedRecommendation(fresh, clock.instant().plus(ttl));
        });
        return cached.tier();
    }

    /**
     * Run every registered criterion and aggregate the votes into a tier.
     *
     * <p>Per-criterion failures are isolated: any {@link RuntimeException}
     * thrown during aggregation is logged and the service falls back to
     * {@link MembershipTier#SILVER} so that callers (and dependent
     * subscription operations) never see a thrown exception.
     *
     * @param user user being scored
     * @return the aggregated tier recommendation
     */
    private MembershipTier aggregate(User user) {
        try {
            EnumMap<MembershipTier, Long> scores = new EnumMap<>(MembershipTier.class);
            for (TierCriterion c : registry.all()) {
                TierScore s = c.evaluate(user, clock);
                scores.merge(s.suggestion(), (long) s.strength() * c.weight(), Long::sum);
            }
            return scores.entrySet().stream()
                    .max(Comparator
                            .comparingLong((Map.Entry<MembershipTier, Long> e) -> e.getValue())
                            .thenComparing(e -> -e.getKey().ordinal()))
                    .map(Map.Entry::getKey)
                    .orElse(MembershipTier.SILVER);
        } catch (RuntimeException ex) {
            log.warn("Tier recommendation failed for user {}; defaulting to SILVER", user.getId(), ex);
            return MembershipTier.SILVER;
        }
    }

    /**
     * Drop any cached recommendation for the given user. Intended for tests
     * and operational diagnostics; the service does not invoke this itself.
     *
     * @param userId the user identifier whose cache entry should be cleared
     */
    public void invalidate(String userId) {
        cache.remove(userId);
    }

    /**
     * @return the current size of the recommendation cache; intended for
     *         tests and operational diagnostics
     */
    public int cacheSize() {
        return cache.size();
    }
}
