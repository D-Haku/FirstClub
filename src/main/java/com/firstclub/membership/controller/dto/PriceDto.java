package com.firstclub.membership.controller.dto;

import com.firstclub.membership.domain.MembershipPlan;
import com.firstclub.membership.domain.MembershipTier;

import java.math.BigDecimal;

/**
 * REST projection of a single {@code PlanTierPrice} row in the catalog response.
 *
 * <p>Mirrors the JSON shape documented in the design's REST API Contract section:
 * the (plan, tier) combination, the catalog amount as a {@link BigDecimal}, and
 * the ISO-4217 3-letter currency code.
 *
 * <p>Validates: Requirements 1.1, 1.5, 10.4, 10.5.
 *
 * @param plan     the membership plan this price is for
 * @param tier     the membership tier this price is for
 * @param amount   the catalog amount; preserved as {@link BigDecimal} to avoid
 *                 floating-point drift on the wire
 * @param currency the ISO-4217 3-letter currency code (e.g., {@code USD})
 */
public record PriceDto(
        MembershipPlan plan,
        MembershipTier tier,
        BigDecimal amount,
        String currency) {
}
