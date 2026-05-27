package com.firstclub.membership.controller.dto;

import com.firstclub.membership.benefit.BenefitFailure;
import com.firstclub.membership.domain.BenefitId;

/**
 * REST projection of a single {@link BenefitFailure} audit entry on the checkout response.
 *
 * <p>Per Requirement 7.4, benefit failures are surfaced under the {@code failures} field
 * of the checkout response rather than as an HTTP error so a single misbehaving benefit
 * cannot fail the whole checkout.
 *
 * <p>Validates: Requirements 7.4, 8.4, 10.4, 10.5.
 *
 * @param id     the benefit identifier
 * @param reason short description of why the benefit failed; typically the thrown
 *               exception's message
 */
public record BenefitFailureDto(BenefitId id, String reason) {

    /**
     * Project a domain {@link BenefitFailure} into its REST DTO.
     *
     * @param f the failure record to project; must not be {@code null}
     * @return a fully populated DTO
     */
    public static BenefitFailureDto from(BenefitFailure f) {
        return new BenefitFailureDto(f.id(), f.reason());
    }
}
