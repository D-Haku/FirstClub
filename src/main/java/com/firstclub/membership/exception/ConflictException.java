package com.firstclub.membership.exception;

/**
 * Thrown when an operation cannot complete because of a concurrency or state conflict.
 * The {@code GlobalExceptionHandler} wraps the following lower-level signals as conflicts:
 * <ul>
 *   <li>{@code OptimisticLockException} from the {@code @Version} field on
 *       {@code Subscription} when a concurrent tier change loses the race.</li>
 *   <li>{@code DataIntegrityViolationException} from the partial unique index that enforces
 *       at most one ACTIVE subscription per user.</li>
 *   <li>Idempotency hash mismatches where a replayed key carries a different request body.</li>
 * </ul>
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to HTTP <strong>409 Conflict</strong>, per the
 * Error Handling section of the design document and the status table in the API contract.
 */
public class ConflictException extends RuntimeException {

    /**
     * Create a conflict exception with a human-readable message describing the conflicting
     * state.
     *
     * @param message the conflict message
     */
    public ConflictException(String message) {
        super(message);
    }

    /**
     * Create a conflict exception that wraps the originating concurrency or persistence
     * failure (for example, an {@code OptimisticLockException} or a
     * {@code DataIntegrityViolationException}).
     *
     * @param message the conflict message
     * @param cause   the underlying failure
     */
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
