package com.firstclub.membership.controller.dto;

import com.firstclub.membership.domain.BenefitId;
import com.firstclub.membership.service.SubscriptionView;

import java.util.List;

/**
 * REST response body for {@code GET /api/v1/subscriptions/{userId}/current}.
 *
 * <p>The endpoint returns 200 OK in both the active and inactive cases, distinguishing
 * them via the {@code active} boolean per the API contract. When inactive, the
 * {@code subscription} and {@code benefits} fields are {@code null}.
 *
 * <p>The DTO reuses {@link SubscriptionView} directly rather than redefining a parallel
 * record, since the view is already serializable as JSON via Jackson and is the
 * canonical service-layer projection.
 *
 * <p>Validates: Requirements 5.1, 5.2, 1.4, 10.4, 10.5.
 *
 * @param active       {@code true} when the user has an active subscription, {@code false}
 *                     otherwise
 * @param subscription the active subscription projection, or {@code null} when inactive
 * @param benefits     the configured benefits for the active tier in execution order, or
 *                     {@code null} when inactive
 */
public record CurrentMembershipResponse(
        boolean active,
        SubscriptionView subscription,
        List<BenefitId> benefits) {

    /**
     * Build the inactive-membership response, leaving the subscription and benefit
     * fields {@code null}.
     *
     * @return a fully populated inactive response
     */
    public static CurrentMembershipResponse inactive() {
        return new CurrentMembershipResponse(false, null, null);
    }

    /**
     * Build the active-membership response carrying the supplied view and benefit list.
     *
     * @param v        the active subscription projection
     * @param benefits the configured benefits for the active tier in execution order
     * @return a fully populated active response
     */
    public static CurrentMembershipResponse active(SubscriptionView v, List<BenefitId> benefits) {
        return new CurrentMembershipResponse(true, v, benefits);
    }
}
