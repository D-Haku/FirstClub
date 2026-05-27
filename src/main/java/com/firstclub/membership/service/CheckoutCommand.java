package com.firstclub.membership.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application-level command that captures everything
 * {@link CheckoutIntegrationService#applyMembership(CheckoutCommand)} needs
 * to apply membership benefits to a single checkout request.
 *
 * <p>Controllers translate their transport-layer DTO into this type before
 * invoking the service, keeping HTTP concerns out of the service layer.
 * The {@code idempotencyKey} is client-supplied and routed through
 * {@link IdempotencyService} so retried checkout calls return the
 * originally-computed response without re-applying benefits.
 *
 * <p>Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 9.4.
 *
 * @param userId         owning user identifier; must not be {@code null} or blank
 * @param idempotencyKey optional client-supplied key that deduplicates retried
 *                       checkout calls; a {@code null} or blank value disables
 *                       idempotency for the call
 * @param items          cart contents at the moment of checkout
 * @param subtotal       pre-benefit subtotal of the cart
 * @param deliveryFee    pre-benefit delivery fee of the cart
 */
public record CheckoutCommand(
        String userId,
        String idempotencyKey,
        List<CartItemView> items,
        BigDecimal subtotal,
        BigDecimal deliveryFee) {

    /**
     * A single cart line as supplied by the caller.
     *
     * <p>The view intentionally omits the exclusive-deal eligibility flag
     * carried by the domain {@code CartItem}: benefits in the engine are
     * responsible for flipping that flag, not callers.
     *
     * @param sku       stock-keeping unit identifier
     * @param qty       quantity ordered; must be non-negative
     * @param unitPrice unit price for the SKU
     */
    public record CartItemView(String sku, int qty, BigDecimal unitPrice) {
    }
}
