package com.firstclub.membership.exception;

/**
 * Thrown when a required resource cannot be found, typically when an operation requires an
 * active subscription, user, or other domain entity that does not exist. For example, calling
 * change-tier or cancel for a user with no active subscription, or requesting current
 * membership for an unknown user.
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to HTTP <strong>404 Not Found</strong>, per the
 * Error Handling section of the design document and the status table in the API contract.
 */
public class NotFoundException extends RuntimeException {

    /**
     * Create a not-found exception with a human-readable message identifying the missing
     * resource.
     *
     * @param message the not-found message
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * Create a not-found exception that wraps an underlying cause (e.g., a downstream
     * lookup failure that surfaced as an absence).
     *
     * @param message the not-found message
     * @param cause   the underlying failure
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
