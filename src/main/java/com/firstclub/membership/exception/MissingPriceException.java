package com.firstclub.membership.exception;

/**
 * Thrown when a {@code (plan, tier)} combination has no configured {@code PlanTierPrice} row
 * in the catalog. This represents an unprocessable catalog state: the request itself is
 * well-formed, but the server cannot price the requested combination.
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to HTTP <strong>422 Unprocessable Entity</strong>,
 * per the Error Handling section of the design document and the status table in the API
 * contract.
 *
 * <p>Carries the offending plan and tier identifiers so they can be surfaced in the problem
 * detail response and operator logs.
 */
public class MissingPriceException extends RuntimeException {

    private final String plan;
    private final String tier;

    /**
     * Create a missing-price exception with a fully formed message.
     *
     * @param message the missing-price message
     */
    public MissingPriceException(String message) {
        super(message);
        this.plan = null;
        this.tier = null;
    }

    /**
     * Create a missing-price exception that wraps an underlying cause (e.g., a repository
     * lookup that surfaced an empty result while pricing a subscription).
     *
     * @param message the missing-price message
     * @param cause   the underlying failure
     */
    public MissingPriceException(String message, Throwable cause) {
        super(message, cause);
        this.plan = null;
        this.tier = null;
    }

    /**
     * Create a missing-price exception identifying the catalog combination that is missing.
     * The plan and tier are stored on the exception and also included in the generated
     * message.
     *
     * @param plan the {@code MembershipPlan} identifier (e.g., {@code MONTHLY}); may be any
     *             string or enum {@code name()}
     * @param tier the {@code MembershipTier} identifier (e.g., {@code GOLD}); may be any
     *             string or enum {@code name()}
     */
    public MissingPriceException(String plan, String tier) {
        super("No PlanTierPrice configured for plan=" + plan + ", tier=" + tier);
        this.plan = plan;
        this.tier = tier;
    }

    /**
     * @return the plan identifier supplied at construction, or {@code null} if the exception
     *         was created with a free-form message
     */
    public String getPlan() {
        return plan;
    }

    /**
     * @return the tier identifier supplied at construction, or {@code null} if the exception
     *         was created with a free-form message
     */
    public String getTier() {
        return tier;
    }
}
