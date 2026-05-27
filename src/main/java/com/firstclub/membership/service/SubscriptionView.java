package com.firstclub.membership.service;

import com.firstclub.membership.domain.MembershipPlan;
import com.firstclub.membership.domain.MembershipTier;
import com.firstclub.membership.domain.Subscription;
import com.firstclub.membership.domain.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read-only projection of a {@link Subscription} returned by
 * {@link SubscriptionService}.
 *
 * <p>The view exposes only those fields that callers (controllers, the
 * checkout integration service, and {@link IdempotencyService} when
 * deserializing cached responses) need, decoupling them from the JPA
 * entity and from the package-private setters that exist for tests.
 *
 * <p>Validates: Requirements 2.1, 2.2, 3.1, 4.1, 5.1.
 *
 * @param id           subscription identifier
 * @param userId       owning user identifier
 * @param plan         billing-duration dimension
 * @param tier         benefit-level dimension
 * @param priceCharged catalog price charged at subscribe time
 * @param status       current persisted status; the EXPIRED projection is applied at read
 *                     time by callers that filter on {@code endAt}
 * @param startAt      subscribe instant
 * @param endAt        instant after which the subscription is no longer active
 * @param canceledAt   instant the subscription was canceled, or {@code null} if still active
 * @param version      JPA optimistic-lock version number
 */
public record SubscriptionView(
        String id,
        String userId,
        MembershipPlan plan,
        MembershipTier tier,
        BigDecimal priceCharged,
        SubscriptionStatus status,
        Instant startAt,
        Instant endAt,
        Instant canceledAt,
        long version) {

    /**
     * Build a view from a persisted {@link Subscription}.
     *
     * @param s the entity to project; must not be {@code null}
     * @return a fully populated view
     */
    public static SubscriptionView from(Subscription s) {
        return new SubscriptionView(
                s.getId(),
                s.getUserId(),
                s.getPlan(),
                s.getTier(),
                s.getPriceCharged(),
                s.getStatus(),
                s.getStartAt(),
                s.getEndAt(),
                s.getCanceledAt(),
                s.getVersion());
    }
}
