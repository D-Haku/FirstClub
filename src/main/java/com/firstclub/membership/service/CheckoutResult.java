package com.firstclub.membership.service;

import com.firstclub.membership.benefit.AppliedBenefit;
import com.firstclub.membership.benefit.BenefitFailure;
import com.firstclub.membership.benefit.CheckoutContext;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only projection of the post-benefit checkout state returned by
 * {@link CheckoutIntegrationService#applyMembership(CheckoutCommand)}.
 *
 * <p>The result mirrors the fields of {@link CheckoutContext} that are
 * relevant to API callers: the (possibly mutated) {@code subtotal} and
 * {@code deliveryFee}, the cumulative {@code totalAdjustment}, the
 * {@code prioritySupport} flag, and the audit lists of applied benefits
 * and per-benefit failures. The benefit and failure lists are copied so
 * the result is safe to cache and to serialize via the idempotency
 * subsystem.
 *
 * <p>Validates: Requirements 8.1, 8.2, 8.4.
 *
 * @param subtotal         post-benefit subtotal
 * @param deliveryFee      post-benefit delivery fee
 * @param totalAdjustment  cumulative signed monetary adjustment
 * @param prioritySupport  whether priority support was granted
 * @param appliedBenefits  ordered audit list of successfully applied benefits
 * @param failures         audit list of per-benefit failures recorded during apply
 */
public record CheckoutResult(
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal totalAdjustment,
        boolean prioritySupport,
        List<AppliedBenefit> appliedBenefits,
        List<BenefitFailure> failures) {

    /**
     * Project the supplied {@link CheckoutContext} into an immutable
     * {@code CheckoutResult}.
     *
     * <p>The applied-benefit and failure lists are copied via
     * {@link List#copyOf(java.util.Collection)} so that subsequent
     * mutations on {@code ctx} cannot leak into a returned result.
     *
     * @param ctx the context whose state should be projected
     * @return a fully populated, immutable result
     */
    public static CheckoutResult from(CheckoutContext ctx) {
        return new CheckoutResult(
                ctx.getSubtotal(),
                ctx.getDeliveryFee(),
                ctx.getTotalAdjustment(),
                ctx.isPrioritySupport(),
                List.copyOf(ctx.getApplied()),
                List.copyOf(ctx.getFailures()));
    }
}
