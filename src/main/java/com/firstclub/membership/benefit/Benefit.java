package com.firstclub.membership.benefit;

import com.firstclub.membership.domain.BenefitConfig;
import com.firstclub.membership.domain.BenefitId;

/**
 * Strategy interface for a single benefit in the {@code BenefitEngine} chain.
 *
 * <p>Each implementation is registered as a Spring {@code @Component} and
 * indexed by {@link #id()} in the {@code BenefitRegistry}. The engine looks
 * up the per-tier {@link BenefitConfig} rows in {@code executionOrder} and
 * invokes {@link #apply(CheckoutContext, BenefitConfig)} for each one,
 * passing the row so the strategy can read {@code paramsJson} (for example
 * {@code {"rate":0.05}} for {@code ExtraDiscountBenefit}).
 *
 * <p>Implementations may freely mutate the {@code CheckoutContext} - rewrite
 * items, set {@code deliveryFee}, flip {@code prioritySupport}, accumulate
 * adjustments - and must return an {@link AppliedBenefit} describing what
 * they did. Throwing from {@code apply} is handled by the engine: it records
 * a {@code BenefitFailure} and continues with the next benefit.
 *
 * <p>Validates: Requirements 7.1, 7.3, 7.4.
 */
public interface Benefit {

    /**
     * Returns the {@link BenefitId} this strategy implements. Each
     * {@code BenefitId} value must be implemented by exactly one
     * {@code Benefit} bean so the registry can route lookups.
     */
    BenefitId id();

    /**
     * Applies this benefit to the supplied checkout context, optionally
     * reading parameters from the per-tier {@link BenefitConfig} row.
     *
     * <p>Implementations whose behavior is fully fixed (for example
     * {@code FreeDeliveryBenefit}) may ignore {@code config}. Implementations
     * that read parameters from {@link BenefitConfig#getParamsJson()} are
     * responsible for parsing and validating the JSON; a parse error should
     * be surfaced as a thrown {@code RuntimeException} so the engine can
     * record a {@code BenefitFailure}.
     *
     * @param ctx the per-request checkout context to read from and mutate.
     * @param config the per-tier configuration row that selected this
     *     benefit. May carry {@code null} {@code paramsJson}.
     * @return an {@link AppliedBenefit} describing the effect of this
     *     benefit on the context.
     */
    AppliedBenefit apply(CheckoutContext ctx, BenefitConfig config);
}
