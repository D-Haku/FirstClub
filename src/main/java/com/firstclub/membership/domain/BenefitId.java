package com.firstclub.membership.domain;

/**
 * Identifier of a benefit strategy that the {@code BenefitEngine} can apply to a checkout.
 *
 * <p>Each value corresponds to a concrete {@code Benefit} implementation
 * registered with the {@code BenefitRegistry} and configured per tier
 * through {@code BenefitConfig} rows.
 *
 * <ul>
 *   <li>{@link #FREE_DELIVERY} - waives the delivery fee.</li>
 *   <li>{@link #EXTRA_DISCOUNT} - applies a percentage discount on the cart subtotal.</li>
 *   <li>{@link #EXCLUSIVE_DEALS} - marks eligible cart items and records a line-level adjustment.</li>
 *   <li>{@link #PRIORITY_SUPPORT} - records a non-monetary priority-support flag.</li>
 * </ul>
 *
 * <p>Validates: Requirements 1.4, 7.1.
 */
public enum BenefitId {

    /** Free delivery benefit. Sets {@code deliveryFee} to zero. */
    FREE_DELIVERY,

    /** Extra discount benefit. Applies a percentage discount on subtotal. */
    EXTRA_DISCOUNT,

    /** Exclusive deals benefit. Marks eligible cart items. */
    EXCLUSIVE_DEALS,

    /** Priority support benefit. Non-monetary; records a flag on the context. */
    PRIORITY_SUPPORT
}
