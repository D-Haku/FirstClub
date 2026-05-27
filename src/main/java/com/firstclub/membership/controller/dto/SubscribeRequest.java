package com.firstclub.membership.controller.dto;

import com.firstclub.membership.domain.MembershipPlan;
import com.firstclub.membership.domain.MembershipTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * REST request body for {@code POST /api/v1/subscriptions}.
 *
 * <p>Validation is enforced by Jakarta Bean Validation on the controller boundary; the
 * service layer receives the already-validated values via {@code SubscribeCommand}.
 * The {@code idempotencyKey} is optional - a {@code null} or blank value disables
 * idempotency for the call as documented in the design.
 *
 * <p>Validates: Requirements 2.1, 2.4, 9.4, 10.4, 10.5.
 *
 * @param userId         owning user identifier; must not be blank
 * @param plan           billing-duration dimension; must not be {@code null}
 * @param tier           benefit-level dimension; must not be {@code null}
 * @param idempotencyKey optional client-supplied key that deduplicates retried subscribe calls
 */
public record SubscribeRequest(
        @NotBlank String userId,
        @NotNull MembershipPlan plan,
        @NotNull MembershipTier tier,
        String idempotencyKey) {
}
