package com.firstclub.membership.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST request body for {@code POST /api/v1/checkout/apply-membership}.
 *
 * <p>Mirrors the JSON contract documented in the design's REST API Contract section.
 * Validation is enforced by Jakarta Bean Validation on the controller boundary; the
 * service layer receives the already-validated values via {@code CheckoutCommand}.
 *
 * <p>Validates: Requirements 8.1, 8.2, 8.3, 8.5, 9.4, 10.4, 10.5.
 *
 * @param userId         owning user identifier; must not be blank
 * @param idempotencyKey optional client-supplied key that deduplicates retried checkout calls
 * @param cart           the cart contents and totals at the moment of checkout; must not be
 *                       {@code null}
 */
public record CheckoutRequest(
        @NotBlank String userId,
        String idempotencyKey,
        @NotNull @Valid CartDto cart) {

    /**
     * The cart contents and pre-benefit totals supplied to the checkout endpoint.
     *
     * @param items       cart line items
     * @param subtotal    pre-benefit subtotal of the cart
     * @param deliveryFee pre-benefit delivery fee of the cart
     */
    public record CartDto(
            @NotNull @Valid List<ItemDto> items,
            @NotNull BigDecimal subtotal,
            @NotNull BigDecimal deliveryFee) {
    }

    /**
     * A single cart line as supplied by the caller.
     *
     * @param sku       stock-keeping unit identifier; must not be blank
     * @param qty       quantity ordered
     * @param unitPrice unit price for the SKU
     */
    public record ItemDto(
            @NotBlank String sku,
            int qty,
            @NotNull BigDecimal unitPrice) {
    }
}
