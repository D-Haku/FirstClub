package com.firstclub.membership.repository;

import com.firstclub.membership.domain.BenefitConfig;
import com.firstclub.membership.domain.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link BenefitConfig} rows.
 *
 * <p>The benefit chain for a given tier is materialized in deterministic
 * order via {@link #findByTierOrderByExecutionOrderAsc(MembershipTier)},
 * which the {@code BenefitRegistry} composes when applying benefits.
 *
 * <p>Validates: Requirements 1.4, 5.1, 7.1.
 */
@Repository
public interface BenefitConfigRepository extends JpaRepository<BenefitConfig, Long> {

    /**
     * Returns all benefit configurations for the given tier, ordered by
     * ascending {@code executionOrder} so the chain runs in a stable,
     * deterministic sequence.
     *
     * @param tier the membership tier
     * @return ordered list of benefit configurations; empty if none defined
     */
    List<BenefitConfig> findByTierOrderByExecutionOrderAsc(MembershipTier tier);
}
