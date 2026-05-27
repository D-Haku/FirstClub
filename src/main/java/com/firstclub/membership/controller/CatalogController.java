package com.firstclub.membership.controller;

import com.firstclub.membership.controller.dto.CatalogResponse;
import com.firstclub.membership.controller.dto.PlanDto;
import com.firstclub.membership.controller.dto.PriceDto;
import com.firstclub.membership.controller.dto.TierDto;
import com.firstclub.membership.domain.BenefitId;
import com.firstclub.membership.service.CatalogSnapshot;
import com.firstclub.membership.service.MembershipCatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the public catalog endpoint.
 *
 * <p>Exposes {@code GET /api/v1/catalog} which returns the membership catalog
 * (plans, tiers with their configured benefits in execution order, per-(plan,
 * tier) prices) and an optional tier recommendation when the caller supplies a
 * {@code userId} query parameter.
 *
 * <p>The controller is intentionally thin: it delegates all business logic to
 * {@link MembershipCatalogService} and only translates the resulting
 * {@link CatalogSnapshot} into the wire-level {@link CatalogResponse} DTO.
 *
 * <p>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 10.4, 10.5.
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final MembershipCatalogService catalogService;

    /**
     * Constructs the controller with its single collaborator.
     *
     * @param catalogService application service that assembles a catalog snapshot
     */
    public CatalogController(MembershipCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Render the membership catalog as a {@link CatalogResponse}.
     *
     * <p>When {@code userId} is supplied and resolves to a known user, the
     * response includes a {@code recommendedTier}; otherwise that field is
     * {@code null}.
     *
     * @param userId optional caller identifier for tier recommendation
     * @return the catalog response payload
     */
    @GetMapping("")
    public CatalogResponse getCatalog(@RequestParam(value = "userId", required = false) String userId) {
        CatalogSnapshot snapshot = catalogService.getCatalog(userId);

        List<PlanDto> plans = snapshot.plans().stream()
                .map(PlanDto::of)
                .toList();

        List<TierDto> tiers = snapshot.tierBenefits().keySet().stream()
                .map(tier -> {
                    List<BenefitId> benefits = snapshot.tierBenefits().get(tier);
                    return new TierDto(tier, tier.rank(), benefits);
                })
                .toList();

        List<PriceDto> prices = snapshot.prices().stream()
                .map(p -> new PriceDto(p.plan(), p.tier(), p.amount(), p.currency()))
                .toList();

        return new CatalogResponse(plans, tiers, prices, snapshot.recommendedTier());
    }
}
