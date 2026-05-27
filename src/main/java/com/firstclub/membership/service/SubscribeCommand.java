package com.firstclub.membership.service;

import com.firstclub.membership.domain.MembershipPlan;
import com.firstclub.membership.domain.MembershipTier;

/**
 * Application-level command that captures everything {@link SubscriptionService#subscribe}
 * needs to create a new subscription.
 *
 * <p>The command is intentionally a thin record: controllers convert their
 * transport-layer DTO into this type before invoking the service, keeping HTTP and
 * validation concerns out of the service layer. The {@code idempotencyKey} is
 * client-supplied and routed through {@link IdempotencyService} so that retried
 * requests do not produce duplicate subscriptions.
 *
 * <p>Validates: Requirements 2.1, 2.2, 2.4, 9.4.
 *
 * @param userId         owning user identifier; must not be {@code null} or blank
 * @param plan           billing-duration dimension of the membership; must not be {@code null}
 * @param tier           benefit-level dimension of the membership; must not be {@code null}
 * @param idempotencyKey optional client-supplied key that deduplicates retried subscribe calls;
 *                       a {@code null} or blank value disables idempotency for the call
 */
public record SubscribeCommand(
        String userId,
        MembershipPlan plan,
        MembershipTier tier,
        String idempotencyKey) {
}
