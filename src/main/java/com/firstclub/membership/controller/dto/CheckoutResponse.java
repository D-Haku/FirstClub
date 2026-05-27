package com.firstclub.membership.controller.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST response body for {@code POST /api/v1/checkout/apply-membership}.
 *
 * <p>Mirrors the JSON contract documented in the design's REST API Contract section.
 * The fields are projected from the {@code CheckoutContext} returned by the
 * {@code BenefitEngine} so the response surface matches the engine output exactly,
 * per Property 19.
 *
 * <p>Validates: Requirements 7.1, 7.3, 7.4, 8.1, 8.2, 8.4, 10.4, 10.5.
 *
 * @param subtotal         post-benefit cart subtotal
 * @param deliveryFee      post-benefit delivery fee
 * @param totalAdjustment  cumulative signed monetary adjustment applied across all benefits
 * @param prioritySupport  whether priority support was granted
 * @param appliedBenefits  ordered audit list of successfully applied benefits
 * @param failures         audit list of per-benefit failures recorded during apply
 */
public record CheckoutResponse(
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal totalAdjustment,
        boolean prioritySupport,
        List<AppliedBenefitDto> appliedBenefits,
        List<BenefitFailureDto> failures) {
}
