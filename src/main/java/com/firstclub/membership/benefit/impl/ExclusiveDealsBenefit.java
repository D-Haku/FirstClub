package com.firstclub.membership.benefit.impl;

import com.firstclub.membership.benefit.AppliedBenefit;
import com.firstclub.membership.benefit.Benefit;
import com.firstclub.membership.benefit.CartItem;
import com.firstclub.membership.benefit.CheckoutContext;
import com.firstclub.membership.domain.BenefitConfig;
import com.firstclub.membership.domain.BenefitId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Marks every cart line as eligible for exclusive deals and records a
 * line-level marketing reward.
 *
 * <p>For demo purposes, every {@link CartItem} is rebuilt with
 * {@code eligibleForExclusiveDeal=true} (preserving its sku, quantity, and
 * unit price) and the new list is installed via
 * {@link CheckoutContext#replaceItems(java.util.List)}. The strategy then
 * accumulates a flat 5% per-line reward computed as
 * {@code unitPrice * 0.05} per line (independent of quantity, matching the
 * task description) and records the negation of that total as the
 * adjustment on the context.
 *
 * <p>Monetary values are normalized to scale 4 with
 * {@link RoundingMode#HALF_UP} to keep totals consistent across the chain.
 *
 * <p>Validates: Requirements 7.1, 8.1.
 */
@Component
public class ExclusiveDealsBenefit implements Benefit {

    private static final int MONEY_SCALE = 4;
    private static final BigDecimal LINE_REWARD_RATE =
            new BigDecimal("0.05").setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    @Override
    public BenefitId id() {
        return BenefitId.EXCLUSIVE_DEALS;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Re-installs the cart with every line marked eligible and totals a
     * 5%-of-{@code unitPrice} marketing reward per line. The
     * {@link BenefitConfig} parameter is unused: this benefit takes no
     * tier-specific parameters.
     */
    @Override
    public AppliedBenefit apply(CheckoutContext ctx, BenefitConfig config) {
        List<CartItem> currentItems = ctx.getItems();
        List<CartItem> rebuilt = new ArrayList<>(currentItems.size());
        BigDecimal sum = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        for (CartItem item : currentItems) {
            rebuilt.add(item.withEligibility(true));
            BigDecimal lineReward = item.unitPrice()
                    .multiply(LINE_REWARD_RATE)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            sum = sum.add(lineReward).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        ctx.replaceItems(rebuilt);

        BigDecimal adjustment = sum.negate().setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        String description = "Exclusive deals applied to " + rebuilt.size() + " items";
        return new AppliedBenefit(BenefitId.EXCLUSIVE_DEALS, adjustment, description);
    }
}
