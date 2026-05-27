package com.firstclub.membership.tier.impl;

import com.firstclub.membership.domain.MembershipTier;
import com.firstclub.membership.domain.User;
import com.firstclub.membership.tier.TierCriterion;
import com.firstclub.membership.tier.TierScore;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Tier-recommendation criterion that votes based on the User's cohort
 * label, which is treated as an explicit business override signal.
 *
 * <p>The criterion declares a {@link #weight()} of {@code 2} so that an
 * explicit cohort assignment can outweigh a single behavioural signal
 * (such as {@code OrderCountCriterion} or {@code MonthlySpendCriterion},
 * each with weight {@code 1}) in the deterministic aggregator.
 *
 * <p>Lookup table:
 * <ul>
 *   <li>{@code "EARLY_ADOPTER"} &rarr; {@link MembershipTier#PLATINUM} with strength 5</li>
 *   <li>{@code "LOYAL"}         &rarr; {@link MembershipTier#GOLD} with strength 3</li>
 *   <li>{@code "NEW"}           &rarr; {@link MembershipTier#SILVER} with strength 1</li>
 *   <li>{@code null} or any other value &rarr; {@link MembershipTier#SILVER} with strength 0</li>
 * </ul>
 *
 * <p>The criterion has no external collaborators and is therefore
 * trivially deterministic and side-effect free.
 *
 * <p>Validates: Requirements 6.1, 6.3.
 */
@Component
public class CohortCriterion implements TierCriterion {

    /** Cohort label for high-affinity early-program participants. */
    static final String EARLY_ADOPTER = "EARLY_ADOPTER";

    /** Cohort label for repeat customers with established loyalty. */
    static final String LOYAL = "LOYAL";

    /** Cohort label for newly-onboarded users. */
    static final String NEW = "NEW";

    @Override
    public String id() {
        return "COHORT";
    }

    @Override
    public int weight() {
        return 2;
    }

    @Override
    public TierScore evaluate(User user, Clock clock) {
        String cohort = user.getCohort();
        if (cohort == null) {
            return new TierScore(MembershipTier.SILVER, 0);
        }
        return switch (cohort) {
            case EARLY_ADOPTER -> new TierScore(MembershipTier.PLATINUM, 5);
            case LOYAL -> new TierScore(MembershipTier.GOLD, 3);
            case NEW -> new TierScore(MembershipTier.SILVER, 1);
            default -> new TierScore(MembershipTier.SILVER, 0);
        };
    }
}
