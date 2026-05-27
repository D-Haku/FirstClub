package com.firstclub.membership.controller.dto;

import com.firstclub.membership.benefit.AppliedBenefit;
import com.firstclub.membership.domain.BenefitId;

import java.math.BigDecimal;

/**
 * REST projection of a single {@link AppliedBenefit} audit entry on the checkout response.
 *
 * <p>Mirrors the JSON contract documented in the design's REST API Contract section: the
 * benefit identifier, the signed monetary adjustment it produced, and a short
 * human-readable description suitable for display.
 *
 * <p>Validates: Requirements 7.1, 7.3, 8.4, 10.4, 10.5.
 *
 * @param id          the benefit identifier
 * @param adjustment  the signed monetary delta the benefit contributed (typically negative
 *                    for discounts and waivers, zero for non-monetary perks)
 * @param description short summary suitable for direct inclusion in API responses
 */
public record AppliedBenefitDto(BenefitId id, BigDecimal adjustment, String description) {

    /**
     * Project a domain {@link AppliedBenefit} into its REST DTO.
     *
     * @param a the benefit audit record to project; must not be {@code null}
     * @return a fully populated DTO
     */
    public static AppliedBenefitDto from(AppliedBenefit a) {
        return new AppliedBenefitDto(a.id(), a.adjustment(), a.description());
    }
}
