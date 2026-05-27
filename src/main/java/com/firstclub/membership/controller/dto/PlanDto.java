package com.firstclub.membership.controller.dto;

import com.firstclub.membership.domain.MembershipPlan;

/**
 * REST projection of a single {@code MembershipPlan} entry in the catalog response.
 *
 * <p>Carries the plan's enum {@code code} and its {@code durationDays} so clients
 * can render a plan picker without depending on internal enum semantics.
 *
 * <p>Validates: Requirements 1.1, 10.4, 10.5.
 *
 * @param code         the plan identifier (e.g., {@code MONTHLY})
 * @param durationDays the plan's billing duration in days
 */
public record PlanDto(MembershipPlan code, int durationDays) {

    /**
     * Build a {@code PlanDto} from a {@link MembershipPlan} enum value.
     *
     * @param p the plan to project; must not be {@code null}
     * @return a fully populated DTO
     */
    public static PlanDto of(MembershipPlan p) {
        return new PlanDto(p, p.durationDays());
    }
}
