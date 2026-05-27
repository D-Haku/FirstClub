package com.firstclub.membership.service;

import com.firstclub.membership.domain.BenefitId;
import com.firstclub.membership.domain.MembershipPlan;
import com.firstclub.membership.domain.MembershipTier;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Immutable value object that captures everything the catalog endpoint needs
 * to render a {@code GET /api/v1/catalog} response.
 *
 * <p>The snapshot is produced by {@link MembershipCatalogService#getCatalog(String)}
 * and consumed by the controller layer, which converts it into the public
 * JSON contract described in the design document.
 *
 * <p>Field semantics:
 * <ul>
 *   <li>{@link #plans} - all known {@link MembershipPlan} values in enum order.</li>
 *   <li>{@link #tierBenefits} - for every {@link MembershipTier} (in enum order),
 *       the configured {@link BenefitId}s in {@code BenefitConfig.executionOrder}.
 *       Backed by a {@link java.util.LinkedHashMap} so iteration order matches
 *       the documented response shape.</li>
 *   <li>{@link #prices} - one {@link PlanTierPriceView} per (plan, tier)
 *       combination present in the {@code plan_tier_price} table.</li>
 *   <li>{@link #recommendedTier} - optional tier suggestion when the request
 *       supplied a {@code userId}; {@code null} otherwise.</li>
 * </ul>
 *
 * <p>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 5.1, 6.1.
 */
public record CatalogSnapshot(
        List<MembershipPlan> plans,
        Map<MembershipTier, List<BenefitId>> tierBenefits,
        List<PlanTierPriceView> prices,
        @Nullable MembershipTier recommendedTier) {

    /**
     * Flattened projection of a single {@code PlanTierPrice} row used in the
     * catalog response. Decoupling the view from the JPA entity keeps the
     * service contract independent of persistence concerns.
     *
     * @param plan     the membership plan this price is for
     * @param tier     the membership tier this price is for
     * @param amount   the catalog amount; preserved as {@link BigDecimal}
     *                 to avoid floating-point drift
     * @param currency the ISO-4217 3-letter currency code
     */
    public record PlanTierPriceView(
            MembershipPlan plan,
            MembershipTier tier,
            BigDecimal amount,
            String currency) {
    }
}
