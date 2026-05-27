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
 * Waives the delivery fee on the {@link CheckoutContext}.
 *
 * <p>The strategy captures the current {@code deliveryFee}, sets it to zero on
 * the context, and records the negation as a signed adjustment. The
 * {@link AppliedBenefit} returned to the engine carries the same negative
 * delta and a human-readable description for inclusion in the API response.
 *
 * <p>Monetary values are normalized to scale 4 with {@link RoundingMode#HALF_UP}
 * to keep totals consistent across the chain.
 *
 * <p>Validates: Requirements 7.1, 8.1.
 */
@Component
public class FreeDeliveryBenefit implements Benefit {

    private static final int MONEY_SCALE = 4;

    @Override
    public BenefitId id() {
        return BenefitId.FREE_DELIVERY;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads {@link CheckoutContext#getDeliveryFee()}, zeroes it, and
     * records the waived amount as a negative {@code totalAdjustment}. The
     * {@link BenefitConfig} parameter is unused: this benefit takes no
     * tier-specific parameters.
     */
    @Override
    public AppliedBenefit apply(CheckoutContext ctx, BenefitConfig config) {
        BigDecimal currentDeliveryFee =
                ctx.getDeliveryFee().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal adjustment =
                currentDeliveryFee.negate().setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        ctx.setDeliveryFee(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));

        String description = "Free delivery waived $" + currentDeliveryFee.toPlainString();
        return new AppliedBenefit(BenefitId.FREE_DELIVERY, adjustment, description);
    }
}
