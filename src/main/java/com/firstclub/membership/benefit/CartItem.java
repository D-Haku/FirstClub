package com.firstclub.membership.benefit;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A single line in the {@code CheckoutContext} cart that the {@code BenefitEngine}
 * can read and rewrite.
 *
 * <p>The {@code eligibleForExclusiveDeal} flag is a per-line marker that
 * benefits such as {@code ExclusiveDealsBenefit} flip on. New items default
 * the flag to {@code false} via {@link #of(String, int, BigDecimal)};
 * benefits never mutate items in place but instead build new {@code CartItem}s
 * through {@link #withEligibility(boolean)} and ask the context to replace the
 * cart list.
 *
 * <p>Validates: Requirements 7.1, 7.3, 7.4.
 *
 * @param sku stock-keeping unit identifier of the line.
 * @param qty quantity ordered.
 * @param unitPrice unit price for the SKU.
 * @param eligibleForExclusiveDeal whether the line is currently flagged as
 *     eligible for an exclusive-deals benefit.
 */
public record CartItem(String sku,
                       int qty,
                       BigDecimal unitPrice,
                       boolean eligibleForExclusiveDeal) {

    /**
     * Canonical constructor with light validation. Null {@code sku} or
     * {@code unitPrice} are rejected; {@code qty} must be non-negative.
     */
    public CartItem {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (qty < 0) {
            throw new IllegalArgumentException("qty must be non-negative: " + qty);
        }
    }

    /**
     * Convenience factory that constructs a {@code CartItem} with
     * {@code eligibleForExclusiveDeal=false}. Most upstream callers (REST
     * DTO mappers, tests) do not know about exclusive-deal eligibility, so
     * they should use this factory and let benefits flip the flag.
     *
     * @param sku stock-keeping unit identifier.
     * @param qty quantity ordered.
     * @param unitPrice unit price for the SKU.
     * @return a new {@code CartItem} with the eligibility flag set to {@code false}.
     */
    public static CartItem of(String sku, int qty, BigDecimal unitPrice) {
        return new CartItem(sku, qty, unitPrice, false);
    }

    /**
     * Returns a copy of this item with {@code eligibleForExclusiveDeal} set
     * to the supplied value. Records are immutable, so this creates a new
     * instance and leaves {@code this} untouched.
     *
     * @param eligible the new value of the eligibility flag.
     * @return a new {@code CartItem} with the requested eligibility.
     */
    public CartItem withEligibility(boolean eligible) {
        return new CartItem(sku, qty, unitPrice, eligible);
    }
}
