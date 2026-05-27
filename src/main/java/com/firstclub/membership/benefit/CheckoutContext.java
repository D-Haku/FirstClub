package com.firstclub.membership.benefit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mutable, request-scoped state passed through the {@code BenefitEngine} chain.
 *
 * <p>Each {@code Benefit} reads from and writes to this context: it can rewrite
 * line items via {@link #replaceItems(List)}, change the {@link #subtotal}
 * or {@link #deliveryFee}, accumulate a running {@link #totalAdjustment} via
 * {@link #addAdjustment(BigDecimal)}, flip non-monetary flags such as
 * {@link #prioritySupport}, and record audit entries through
 * {@link #recordApplied(AppliedBenefit)} and {@link #recordFailure(BenefitFailure)}.
 *
 * <p>The context is intentionally not thread-safe: a single checkout request
 * is processed by exactly one thread, and the {@code BenefitEngine} iterates
 * the chain sequentially.
 *
 * <p>Validates: Requirements 7.1, 7.3, 7.4.
 */
public final class CheckoutContext {

    private final String userId;
    private final List<CartItem> items;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal totalAdjustment;
    private boolean prioritySupport;
    private final List<AppliedBenefit> applied;
    private final List<BenefitFailure> failures;

    /**
     * Creates a new {@code CheckoutContext} for a single checkout request.
     *
     * <p>The {@code items} list is copied defensively so that downstream
     * mutation through {@link #replaceItems(List)} does not affect the
     * caller's list. {@link #totalAdjustment} starts at zero, the priority
     * support flag is {@code false}, and the audit lists are empty.
     *
     * @param userId identifier of the User checking out.
     * @param items initial cart contents.
     * @param subtotal pre-benefit subtotal.
     * @param deliveryFee pre-benefit delivery fee.
     */
    public CheckoutContext(String userId,
                           List<CartItem> items,
                           BigDecimal subtotal,
                           BigDecimal deliveryFee) {
        this.userId = Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(items, "items");
        this.items = new ArrayList<>(items);
        this.subtotal = Objects.requireNonNull(subtotal, "subtotal");
        this.deliveryFee = Objects.requireNonNull(deliveryFee, "deliveryFee");
        this.totalAdjustment = BigDecimal.ZERO;
        this.prioritySupport = false;
        this.applied = new ArrayList<>();
        this.failures = new ArrayList<>();
    }

    /** Returns the User identifier this checkout belongs to. */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns a defensive copy of the current cart items. Callers must not
     * assume the returned list is connected to the context; mutate the cart
     * via {@link #replaceItems(List)} instead.
     */
    public List<CartItem> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Replaces the entire cart items list with the supplied list, which is
     * itself copied defensively.
     *
     * @param newItems the replacement list of cart items.
     */
    public void replaceItems(List<CartItem> newItems) {
        Objects.requireNonNull(newItems, "newItems");
        items.clear();
        items.addAll(newItems);
    }

    /** Returns the current subtotal. */
    public BigDecimal getSubtotal() {
        return subtotal;
    }

    /** Sets the subtotal. */
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = Objects.requireNonNull(subtotal, "subtotal");
    }

    /** Returns the current delivery fee. */
    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    /** Sets the delivery fee. */
    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = Objects.requireNonNull(deliveryFee, "deliveryFee");
    }

    /** Returns the cumulative adjustment recorded by applied benefits. */
    public BigDecimal getTotalAdjustment() {
        return totalAdjustment;
    }

    /**
     * Adds the supplied delta to the cumulative {@link #totalAdjustment}.
     * Negative deltas reduce the cart total (typical for discounts);
     * positive deltas increase it.
     *
     * @param delta signed adjustment to add. Must not be {@code null}.
     */
    public void addAdjustment(BigDecimal delta) {
        Objects.requireNonNull(delta, "delta");
        this.totalAdjustment = this.totalAdjustment.add(delta);
    }

    /** Returns whether priority support has been granted on this checkout. */
    public boolean isPrioritySupport() {
        return prioritySupport;
    }

    /** Sets the priority support flag. */
    public void setPrioritySupport(boolean prioritySupport) {
        this.prioritySupport = prioritySupport;
    }

    /**
     * Returns an unmodifiable view of the applied-benefit audit list, in
     * insertion order. Callers can safely iterate without copying.
     */
    public List<AppliedBenefit> getApplied() {
        return Collections.unmodifiableList(applied);
    }

    /**
     * Returns an unmodifiable view of the failure audit list, in insertion
     * order. Callers can safely iterate without copying.
     */
    public List<BenefitFailure> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    /**
     * Appends an {@link AppliedBenefit} to the audit list and folds its
     * adjustment into {@link #totalAdjustment}. A {@code null} adjustment
     * is treated as a non-monetary benefit and leaves the running total
     * unchanged.
     *
     * @param ab the applied-benefit record to append. Must not be {@code null}.
     */
    public void recordApplied(AppliedBenefit ab) {
        Objects.requireNonNull(ab, "ab");
        applied.add(ab);
        if (ab.adjustment() != null) {
            this.totalAdjustment = this.totalAdjustment.add(ab.adjustment());
        }
    }

    /**
     * Appends a {@link BenefitFailure} to the audit list. The
     * {@code BenefitEngine} calls this when a {@code Benefit} throws so the
     * failure surfaces to the API response without aborting the chain.
     *
     * @param f the failure record to append. Must not be {@code null}.
     */
    public void recordFailure(BenefitFailure f) {
        Objects.requireNonNull(f, "f");
        failures.add(f);
    }
}
