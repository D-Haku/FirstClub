package com.firstclub.membership.tier;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registry that holds every {@link TierCriterion} bean discovered at startup.
 *
 * <p>The Spring container injects all available {@link TierCriterion}
 * implementations and the registry exposes them as an immutable
 * {@link List}. The {@code TierRecommendationService} consumes that list
 * to perform deterministic aggregation per
 * {@link com.firstclub.membership.domain.User}.
 *
 * <p>This component owns no mutable state, so it is safe to share across
 * threads. Adding a new criterion is purely additive: declare a new
 * {@code @Component} that implements {@link TierCriterion} and the
 * registry will pick it up on the next context refresh.
 *
 * <p>Validates: Requirements 6.3.
 */
@Component
public class TierCriterionRegistry {

    private final List<TierCriterion> criteria;

    /**
     * Constructs the registry from the discovered criterion beans.
     *
     * @param criteria all {@link TierCriterion} beans contributed to the
     *                 application context; a defensive immutable copy is
     *                 retained so callers cannot mutate the underlying list
     */
    public TierCriterionRegistry(List<TierCriterion> criteria) {
        this.criteria = List.copyOf(criteria);
    }

    /**
     * @return an immutable view of every registered {@link TierCriterion}
     *         in the order Spring resolved them
     */
    public List<TierCriterion> all() {
        return criteria;
    }
}
