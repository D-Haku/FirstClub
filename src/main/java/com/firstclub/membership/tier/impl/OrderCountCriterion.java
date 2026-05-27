package com.firstclub.membership.tier.impl;

import com.firstclub.membership.domain.MembershipTier;
import com.firstclub.membership.domain.User;
import com.firstclub.membership.repository.OrderRepository;
import com.firstclub.membership.tier.TierCriterion;
import com.firstclub.membership.tier.TierScore;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Tier-recommendation criterion that votes based on the trailing 90-day
 * order count for the given {@link User}.
 *
 * <p>The mapping defined by the design is:
 * <ul>
 *   <li>fewer than 5 orders &rarr; {@link MembershipTier#SILVER} with strength 1</li>
 *   <li>between 5 and 14 orders &rarr; {@link MembershipTier#GOLD} with strength 2</li>
 *   <li>15 orders or more &rarr; {@link MembershipTier#PLATINUM} with strength 3</li>
 * </ul>
 *
 * <p>The criterion is deterministic for a fixed repository state and a
 * fixed {@link Clock}: the trailing-window cutoff is derived purely from
 * {@code clock.instant()} and the order count comes from a single
 * repository call.
 *
 * <p>Validates: Requirements 6.1, 6.3.
 */
@Component
public class OrderCountCriterion implements TierCriterion {

    /** Trailing window evaluated for the order-count signal. */
    static final Duration WINDOW = Duration.ofDays(90);

    /** Upper bound (exclusive) for the SILVER bucket. */
    static final long SILVER_MAX_EXCLUSIVE = 5;

    /** Upper bound (exclusive) for the GOLD bucket. */
    static final long GOLD_MAX_EXCLUSIVE = 15;

    private final OrderRepository orderRepository;

    /**
     * Constructs the criterion with its repository collaborator.
     *
     * @param orderRepository repository used to count orders in the trailing window
     */
    public OrderCountCriterion(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public String id() {
        return "ORDER_COUNT";
    }

    @Override
    public int weight() {
        return 1;
    }

    @Override
    public TierScore evaluate(User user, Clock clock) {
        long count = orderRepository.countByUserIdAndPlacedAtAfter(
                user.getId(),
                clock.instant().minus(WINDOW));

        if (count < SILVER_MAX_EXCLUSIVE) {
            return new TierScore(MembershipTier.SILVER, 1);
        }
        if (count < GOLD_MAX_EXCLUSIVE) {
            return new TierScore(MembershipTier.GOLD, 2);
        }
        return new TierScore(MembershipTier.PLATINUM, 3);
    }
}
