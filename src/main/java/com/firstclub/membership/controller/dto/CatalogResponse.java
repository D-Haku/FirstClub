package com.firstclub.membership.controller.dto;

import com.firstclub.membership.domain.MembershipTier;

import java.util.List;

/**
 * REST response payload for {@code GET /api/v1/catalog}.
 *
 * <p>Mirrors the JSON contract documented in the design's REST API Contract
 * section: a flat listing of plans, tiers (with their configured benefits in
 * execution order), per-(plan, tier) prices, and an optional recommended
 * tier when the request supplied a {@code userId}.
 *
 * <p>Validates: Requirements 1.1, 1.2, 1.4, 6.1, 10.4, 10.5.
 *
 * @param plans           every {@code MembershipPlan} in enum order with its duration
 * @param tiers           every {@code MembershipTier} in enum order with its rank and the
 *                        configured benefit identifiers in execution order
 * @param prices          one {@link PriceDto} per (plan, tier) combination present in the
 *                        catalog
 * @param recommendedTier optional tier suggestion; {@code null} when the catalog request
 *                        did not include a {@code userId}
 */
public record CatalogResponse(
        List<PlanDto> plans,
        List<TierDto> tiers,
        List<PriceDto> prices,
        MembershipTier recommendedTier) {
}
