package com.firstclub.membership.repository;

import com.firstclub.membership.domain.MembershipPlan;
import com.firstclub.membership.domain.MembershipTier;
import com.firstclub.membership.domain.PlanTierPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PlanTierPrice} catalog rows.
 *
 * <p>Exposes a lookup by ({@link MembershipPlan}, {@link MembershipTier})
 * which corresponds to the unique constraint on the underlying table.
 *
 * <p>Validates: Requirements 1.1, 1.5, 2.2.
 */
@Repository
public interface PlanTierPriceRepository extends JpaRepository<PlanTierPrice, Long> {

    /**
     * Looks up the catalog price row for the given plan and tier.
     *
     * @param plan the membership plan
     * @param tier the membership tier
     * @return the matching {@link PlanTierPrice}, or {@link Optional#empty()}
     *     if no row exists for this combination
     */
    Optional<PlanTierPrice> findByPlanAndTier(MembershipPlan plan, MembershipTier tier);
}
