package com.firstclub.membership.controller.dto;

import com.firstclub.membership.domain.MembershipTier;
import jakarta.validation.constraints.NotNull;

/**
 * REST request body for {@code POST /api/v1/subscriptions/{userId}/change-tier}.
 *
 * <p>Carries the desired target tier. Same-tier changes are rejected by the service
 * layer with {@code ValidationException}, which the {@code GlobalExceptionHandler}
 * maps to HTTP 400 per the design's API contract.
 *
 * <p>Validates: Requirements 3.1, 3.3, 10.4, 10.5.
 *
 * @param targetTier the membership tier to switch to; must not be {@code null}
 */
public record ChangeTierRequest(@NotNull MembershipTier targetTier) {
}
