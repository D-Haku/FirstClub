package com.firstclub.membership.controller.dto;

import com.firstclub.membership.domain.BenefitId;
import com.firstclub.membership.domain.MembershipTier;

import java.util.List;

/**
 * REST projection of a single {@code MembershipTier} entry in the catalog response.
 *
 * <p>Carries the tier's enum {@code code}, its numeric {@code rank}, and the
 * configured {@link BenefitId}s for that tier in execution order, mirroring the
 * shape documented in the design's REST API Contract section.
 *
 * <p>Validates: Requirements 1.1, 1.4, 5.1, 10.4, 10.5.
 *
 * @param code     the tier identifier (e.g., {@code GOLD})
 * @param rank     the tier's numeric rank
 * @param benefits the configured benefit identifiers in execution order
 */
public record TierDto(MembershipTier code, int rank, List<BenefitId> benefits) {
}
