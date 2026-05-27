package com.firstclub.membership.benefit;

import com.firstclub.membership.domain.MembershipTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Applies the deterministic, per-tier benefit chain to a {@link CheckoutContext}.
 *
 * <p>The engine asks the {@link BenefitRegistry} for the ordered chain of
 * {@link BenefitRegistry.OrderedBenefit} pairs for the supplied
 * {@link MembershipTier} and invokes each {@link Benefit#apply(CheckoutContext,
 * com.firstclub.membership.domain.BenefitConfig)} sequentially. On success the
 * resulting {@link AppliedBenefit} is recorded via
 * {@link CheckoutContext#recordApplied(AppliedBenefit)} (which also folds the
 * benefit's adjustment into {@code totalAdjustment}). If a benefit throws a
 * {@link RuntimeException}, the engine catches it, appends a
 * {@link BenefitFailure} to the context, logs a warning, and continues with
 * the next benefit in the chain so a single misbehaving benefit cannot abort
 * the rest of the checkout.
 *
 * <p>Validates: Requirements 7.1, 7.3, 7.4, 7.5.
 */
@Component
public class BenefitEngine {

    private static final Logger log = LoggerFactory.getLogger(BenefitEngine.class);

    private final BenefitRegistry registry;

    /**
     * @param registry the per-tier benefit chain provider.
     */
    public BenefitEngine(BenefitRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * Applies every benefit configured for {@code tier} to {@code ctx} in
     * {@code executionOrder} ascending. Successful applications are appended
     * to {@link CheckoutContext#getApplied()}; per-benefit
     * {@link RuntimeException}s are translated into {@link BenefitFailure}
     * audit entries and the chain continues.
     *
     * @param ctx the checkout context to mutate.
     * @param tier the active membership tier whose chain should be applied.
     * @return the same {@code ctx} for fluent use by callers.
     */
    public CheckoutContext apply(CheckoutContext ctx, MembershipTier tier) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(tier, "tier");
        for (BenefitRegistry.OrderedBenefit ob : registry.benefitsFor(tier)) {
            try {
                AppliedBenefit applied = ob.benefit().apply(ctx, ob.config());
                ctx.recordApplied(applied);
            } catch (RuntimeException ex) {
                String reason = ex.getMessage() == null
                        ? ex.getClass().getSimpleName()
                        : ex.getMessage();
                ctx.recordFailure(new BenefitFailure(ob.benefit().id(), reason));
                log.warn("Benefit {} failed: {}", ob.benefit().id(), reason, ex);
            }
        }
        return ctx;
    }
}
