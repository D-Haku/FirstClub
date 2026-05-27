package com.firstclub.membership.exception;

/**
 * Thrown when an incoming request fails business or input validation, including unknown enum
 * values, unknown user references, missing required fields, or semantically invalid operations
 * (for example, requesting a tier change to the same tier that is already active).
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to HTTP <strong>400 Bad Request</strong> with an
 * RFC 7807 problem detail of {@code type=validation}, per the Error Handling section of the
 * design document and the status table in the API contract.
 */
public class ValidationException extends RuntimeException {

    /**
     * Create a validation exception with a human-readable message describing the offending
     * field, value, or condition.
     *
     * @param message the validation failure message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Create a validation exception that wraps an underlying cause (e.g., a parsing or
     * constraint violation triggered while validating input).
     *
     * @param message the validation failure message
     * @param cause   the underlying failure
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
