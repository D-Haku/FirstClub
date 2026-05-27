package com.firstclub.membership.benefit;

import com.firstclub.membership.domain.BenefitId;

/**
 * Audit record produced when a {@code Benefit} throws while being applied.
 *
 * <p>The {@code BenefitEngine} catches per-benefit exceptions, appends a
 * {@code BenefitFailure} to {@code CheckoutContext.failures}, and continues
 * with the rest of the chain so a single misbehaving benefit cannot fail
 * the whole checkout. The {@code reason} is taken from the exception
 * message at the engine boundary.
 *
 * <p>Validates: Requirements 7.4.
 *
 * @param id identifier of the benefit that failed.
 * @param reason short description of why the benefit failed; typically the
 *     thrown exception's message.
 */
public record BenefitFailure(BenefitId id, String reason) {
}
