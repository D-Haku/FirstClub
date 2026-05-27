package com.firstclub.membership.benefit;

import com.firstclub.membership.domain.BenefitId;
import java.math.BigDecimal;

/**
 * Audit record produced by a successfully applied {@code Benefit}.
 *
 * <p>Every entry in {@code CheckoutContext.applied} corresponds to one
 * non-failing benefit invocation in the chain ordered by
 * {@code BenefitConfig.executionOrder}. The {@code adjustment} is the signed
 * monetary delta the benefit contributed (typically negative for discounts
 * and waivers, zero for non-monetary perks such as priority support).
 *
 * <p>Validates: Requirements 7.1, 7.3.
 *
 * @param id identifier of the benefit that produced this record.
 * @param adjustment signed monetary impact on the cart total. May be zero
 *     for non-monetary benefits and {@code null} when the benefit elects
 *     not to alter totals.
 * @param description short human-readable summary suitable for inclusion
 *     in API responses (for example, "Free delivery waived $5.00").
 */
public record AppliedBenefit(BenefitId id,
                             BigDecimal adjustment,
                             String description) {
}
