package com.firstclub.membership.domain;

/**
 * Benefit-level dimension of a membership.
 *
 * <p>Each tier exposes a numeric {@link #rank()} used for ordering
 * (higher rank means a richer benefit set) and tie-breaking inside
 * the deterministic tier-recommendation aggregator.
 *
 * <ul>
 *   <li>{@link #SILVER} - rank 1.</li>
 *   <li>{@link #GOLD} - rank 2.</li>
 *   <li>{@link #PLATINUM} - rank 3.</li>
 * </ul>
 *
 * <p>Validates: Requirements 1.1, 1.4, 7.1.
 */
public enum MembershipTier {

    /** Entry-level tier. */
    SILVER(1),

    /** Mid-level tier. */
    GOLD(2),

    /** Top-level tier. */
    PLATINUM(3);

    private final int rank;

    MembershipTier(int rank) {
        this.rank = rank;
    }

    /**
     * @return the numeric rank of this tier (higher means richer benefits).
     */
    public int rank() {
        return rank;
    }
}
