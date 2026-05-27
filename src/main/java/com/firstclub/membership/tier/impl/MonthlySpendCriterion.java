package com.firstclub.membership.tier.impl;

import com.firstclub.membership.domain.MembershipTier;
import com.firstclub.membership.domain.User;
import com.firstclub.membership.repository.OrderRepository;
import com.firstclub.membership.tier.TierCriterion;
import com.firstclub.membership.tier.TierScore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;

/**
 * Tier-recommendation criterion that votes based on the average monthly
 * spend over the trailing 6 months for the given {@link User}.
 *
 * <p>The criterion sums every {@code Order.total} placed in the trailing
 * 180 days and divides by 6 (with {@link RoundingMode#HALF_UP} at scale 2)
 * to compute the user's average monthly spend. The resulting average is
 * mapped to a tier suggestion as follows:
 * <ul>
 *   <li>less than {@code $20} &rarr; {@link MembershipTier#SILVER} with strength 1</li>
 *   <li>{@code $20.00} through {@code $99.99} &rarr; {@link MembershipTier#GOLD} with strength 2</li>
 *   <li>{@code $100} or more &rarr; {@link MembershipTier#PLATINUM} with strength 3</li>
 * </ul>
 *
 * <p>{@code OrderRepository.sumTotalByUserIdAndPlacedAtAfter} returns
 * {@link BigDecimal#ZERO} (via {@code COALESCE}) for users with no
 * trailing orders, so the criterion is null-safe end to end.
 *
 * <p>The criterion is deterministic given the same repository state and
 * the same {@link Clock} reading.
 *
 * <p>Validates: Requirements 6.1, 6.3.
 */
@Component
public class MonthlySpendCriterion implements TierCriterion {

    /** Trailing window evaluated for the spend signal. */
    static final Duration WINDOW = Duration.ofDays(180);

    /** Number of months the trailing window represents, used for averaging. */
    static final BigDecimal MONTHS = new BigDecimal(6);

    /** Lower bound (inclusive) for the GOLD bucket, in dollars. */
    static final BigDecimal GOLD_MIN_INCLUSIVE = new BigDecimal("20");

    /** Lower bound (inclusive) for the PLATINUM bucket, in dollars. */
    static final BigDecimal PLATINUM_MIN_INCLUSIVE = new BigDecimal("100");

    private final OrderRepository orderRepository;

    /**
     * Constructs the criterion with its repository collaborator.
     *
     * @param orderRepository repository used to sum order totals in the trailing window
     */
    public MonthlySpendCriterion(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public String id() {
        return "MONTHLY_SPEND";
    }

    @Override
    public int weight() {
        return 1;
    }

    @Override
    public TierScore evaluate(User user, Clock clock) {
        BigDecimal sum = orderRepository.sumTotalByUserIdAndPlacedAtAfter(
                user.getId(),
                clock.instant().minus(WINDOW));
        BigDecimal averageMonthlySpend = sum.divide(MONTHS, 2, RoundingMode.HALF_UP);

        if (averageMonthlySpend.compareTo(GOLD_MIN_INCLUSIVE) < 0) {
            return new TierScore(MembershipTier.SILVER, 1);
        }
        if (averageMonthlySpend.compareTo(PLATINUM_MIN_INCLUSIVE) < 0) {
            return new TierScore(MembershipTier.GOLD, 2);
        }
        return new TierScore(MembershipTier.PLATINUM, 3);
    }
}
