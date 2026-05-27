package com.firstclub.membership.benefit.impl;

import com.firstclub.membership.benefit.AppliedBenefit;
import com.firstclub.membership.benefit.Benefit;
import com.firstclub.membership.benefit.CheckoutContext;
import com.firstclub.membership.domain.BenefitConfig;
import com.firstclub.membership.domain.BenefitId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Non-monetary benefit that flips {@code prioritySupport=true} on the
 * {@link CheckoutContext}.
 *
 * <p>The adjustment is always zero (normalized to scale 4 with
 * {@link RoundingMode#HALF_UP} for consistency with the rest of the chain).
 * The {@link BenefitConfig} parameter is unused: this benefit takes no
 * tier-specific parameters.
 *
 * <p>Validates: Requirements 7.1.
 */
@Component
public class PrioritySupportBenefit implements Benefit {

    private static final int MONEY_SCALE = 4;
    private static final BigDecimal ZERO_ADJUSTMENT =
            BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    @Override
    public BenefitId id() {
        return BenefitId.PRIORITY_SUPPORT;
    }

    @Override
    public AppliedBenefit apply(CheckoutContext ctx, BenefitConfig config) {
        ctx.setPrioritySupport(true);
        return new AppliedBenefit(
                BenefitId.PRIORITY_SUPPORT,
                ZERO_ADJUSTMENT,
                "Priority support enabled");
    }
}
