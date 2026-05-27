package com.firstclub.membership.domain;

/**
 * Billing-duration dimension of a membership.
 *
 * <p>Each plan exposes its duration in days, which the
 * {@link Subscription} aggregate uses to compute {@code endAt} from
 * {@code startAt} at subscribe time.
 *
 * <ul>
 *   <li>{@link #MONTHLY} - 30 days.</li>
 *   <li>{@link #QUARTERLY} - 90 days.</li>
 *   <li>{@link #YEARLY} - 365 days.</li>
 * </ul>
 *
 * <p>Validates: Requirements 1.1, 2.1, 7.1.
 */
public enum MembershipPlan {

    /** Monthly plan, 30-day billing duration. */
    MONTHLY(30),

    /** Quarterly plan, 90-day billing duration. */
    QUARTERLY(90),

    /** Yearly plan, 365-day billing duration. */
    YEARLY(365);

    private final int durationDays;

    MembershipPlan(int durationDays) {
        this.durationDays = durationDays;
    }

    /**
     * @return the plan's billing duration in days.
     */
    public int durationDays() {
        return durationDays;
    }
}
