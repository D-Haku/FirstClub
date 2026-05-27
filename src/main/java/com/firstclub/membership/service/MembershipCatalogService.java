package com.firstclub.membership.service;

import com.firstclub.membership.domain.BenefitConfig;
import com.firstclub.membership.domain.BenefitId;
import com.firstclub.membership.domain.MembershipPlan;
import com.firstclub.membership.domain.MembershipTier;
import com.firstclub.membership.domain.PlanTierPrice;
import com.firstclub.membership.exception.MissingPriceException;
import com.firstclub.membership.repository.BenefitConfigRepository;
import com.firstclub.membership.repository.PlanTierPriceRepository;
import com.firstclub.membership.repository.UserRepository;
import com.firstclub.membership.service.CatalogSnapshot.PlanTierPriceView;
import com.firstclub.membership.tier.TierRecommendationService;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application service that assembles a single {@link CatalogSnapshot} for the
 * {@code GET /api/v1/catalog} endpoint.
 *
 * <p>The snapshot combines:
 * <ul>
 *   <li>every {@link MembershipPlan} (in enum order),</li>
 *   <li>the ordered {@link BenefitId} chain configured for every
 *       {@link MembershipTier} via {@code BenefitConfig.executionOrder},</li>
 *   <li>one {@link PlanTierPriceView} per priced (plan, tier) pair, and</li>
 *   <li>an optional {@link MembershipTier} recommendation derived from the
 *       caller's {@code userId} when present.</li>
 * </ul>
 *
 * <p>The service treats tier recommendation as advisory: if the supplied
 * {@code userId} does not resolve to a {@link com.firstclub.membership.domain.User},
 * the recommendation is simply omitted rather than failing the request, which
 * preserves the contract that the catalog must remain usable for anonymous and
 * unknown callers.
 *
 * <p>If any (plan, tier) combination has no price row, a
 * {@link MissingPriceException} is raised so the global exception handler can
 * surface it as HTTP 422 per the API contract.
 *
 * <p>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 5.1, 6.1.
 */
@Service
public class MembershipCatalogService {

    private static final Logger log = LoggerFactory.getLogger(MembershipCatalogService.class);

    private final PlanTierPriceRepository priceRepo;
    private final BenefitConfigRepository benefitConfigRepo;
    private final UserRepository userRepo;
    private final TierRecommendationService recommendationService;

    /**
     * Constructs the service with its collaborators.
     *
     * @param priceRepo             repository that exposes catalog price rows
     * @param benefitConfigRepo     repository that exposes per-tier benefit chains
     * @param userRepo              repository used to resolve the optional caller user
     * @param recommendationService strategy aggregator that derives a tier suggestion
     */
    public MembershipCatalogService(PlanTierPriceRepository priceRepo,
                                    BenefitConfigRepository benefitConfigRepo,
                                    UserRepository userRepo,
                                    TierRecommendationService recommendationService) {
        this.priceRepo = priceRepo;
        this.benefitConfigRepo = benefitConfigRepo;
        this.userRepo = userRepo;
        this.recommendationService = recommendationService;
    }

    /**
     * Build a complete {@link CatalogSnapshot} for the given caller.
     *
     * <p>When {@code userId} is non-blank and resolves to a known user, the
     * snapshot's {@code recommendedTier} field is populated using
     * {@link TierRecommendationService#recommend}. Unknown users yield a
     * {@code null} recommendation rather than an error: the recommendation is
     * advisory and must not block the catalog response.
     *
     * @param userId optional caller identifier; may be {@code null} or blank
     * @return a fully populated catalog snapshot
     * @throws MissingPriceException if any (plan, tier) combination is missing
     *     a {@link PlanTierPrice} row
     */
    public CatalogSnapshot getCatalog(@Nullable String userId) {
        List<MembershipPlan> plans = List.of(MembershipPlan.values());

        Map<MembershipTier, List<BenefitId>> tierBenefits = new LinkedHashMap<>();
        for (MembershipTier tier : MembershipTier.values()) {
            List<BenefitId> benefitIds = benefitConfigRepo.findByTierOrderByExecutionOrderAsc(tier).stream()
                    .map(BenefitConfig::getBenefitId)
                    .toList();
            tierBenefits.put(tier, benefitIds);
        }

        List<PlanTierPriceView> prices = priceRepo.findAll().stream()
                .map(p -> new PlanTierPriceView(p.getPlan(), p.getTier(), p.getAmount(), p.getCurrency()))
                .toList();

        validateCatalogComplete(prices);

        MembershipTier recommendedTier = null;
        if (userId != null && !userId.isBlank()) {
            recommendedTier = userRepo.findById(userId)
                    .map(recommendationService::recommend)
                    .orElse(null);
            if (recommendedTier == null) {
                log.debug("Catalog requested for unknown userId={}, recommendation omitted", userId);
            }
        }

        return new CatalogSnapshot(plans, tierBenefits, prices, recommendedTier);
    }

    /**
     * Verify that the given price views cover every (plan, tier) combination.
     * The first missing combination is reported via {@link MissingPriceException}.
     *
     * @param prices price views drawn from the {@code plan_tier_price} table
     * @throws MissingPriceException when a combination has no price row
     */
    private void validateCatalogComplete(List<PlanTierPriceView> prices) {
        for (MembershipPlan plan : MembershipPlan.values()) {
            for (MembershipTier tier : MembershipTier.values()) {
                boolean present = false;
                for (PlanTierPriceView view : prices) {
                    if (view.plan() == plan && view.tier() == tier) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    log.warn("Catalog incomplete: missing price row for plan={}, tier={}", plan, tier);
                    throw new MissingPriceException(plan.name(), tier.name());
                }
            }
        }
    }

}
