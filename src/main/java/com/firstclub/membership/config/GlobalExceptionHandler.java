package com.firstclub.membership.config;

import com.firstclub.membership.exception.ConflictException;
import com.firstclub.membership.exception.MissingPriceException;
import com.firstclub.membership.exception.NotFoundException;
import com.firstclub.membership.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized REST error handler that maps domain exceptions onto RFC 7807
 * Problem+JSON responses, per the Error Handling section of the design document
 * and the status table in the API contract.
 *
 * <p>Mappings:
 * <ul>
 *   <li>{@link ValidationException} &amp; {@link MethodArgumentNotValidException} &rarr; 400</li>
 *   <li>{@link NotFoundException} &rarr; 404</li>
 *   <li>{@link ConflictException}, {@link DataIntegrityViolationException}, and
 *       {@link ObjectOptimisticLockingFailureException} &rarr; 409</li>
 *   <li>{@link MissingPriceException} &rarr; 422</li>
 *   <li>Catch-all {@link Exception} &rarr; 500 with the full stack trace logged at error level</li>
 * </ul>
 *
 * <p>Validates: Requirements 1.5, 2.3, 2.5, 3.3, 3.4, 4.2, 8.5, 9.2, 10.4, 10.5.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI TYPE_VALIDATION = URI.create("about:blank");
    private static final URI TYPE_NOT_FOUND = URI.create("about:blank");
    private static final URI TYPE_CONFLICT = URI.create("about:blank");
    private static final URI TYPE_MISSING_PRICE = URI.create("about:blank");
    private static final URI TYPE_INTERNAL = URI.create("about:blank");

    /**
     * Map a {@link ValidationException} to HTTP 400 Bad Request with
     * {@code type=validation} so clients can distinguish input errors from
     * other 400-class failures.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(ValidationException ex) {
        log.debug("Validation failure: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(TYPE_VALIDATION);
        problem.setTitle("Validation failed");
        problem.setProperty("category", "validation");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Map a {@link MethodArgumentNotValidException} (Jakarta Bean Validation
     * failure on a {@code @RequestBody}) to HTTP 400 with the field errors
     * aggregated into a single {@code errors} map keyed by field name.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a));
        log.debug("Bean validation failure: {}", errors);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setType(TYPE_VALIDATION);
        problem.setTitle("Validation failed");
        problem.setProperty("category", "validation");
        problem.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Map a {@link NotFoundException} to HTTP 404 Not Found.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(TYPE_NOT_FOUND);
        problem.setTitle("Not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * Map a {@link ConflictException} to HTTP 409 Conflict.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        log.debug("Conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(TYPE_CONFLICT);
        problem.setTitle("Conflict");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Catch-all for race-loser inserts that surface as
     * {@link DataIntegrityViolationException} (e.g., the partial unique index that
     * enforces at most one ACTIVE subscription per user). Mapped to HTTP 409.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.debug("Data integrity violation translated to conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Conflicting concurrent change");
        problem.setType(TYPE_CONFLICT);
        problem.setTitle("Conflict");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Map a Spring optimistic-lock failure to HTTP 409 Conflict so the losing
     * caller can retry with a fresh read.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.debug("Optimistic lock failure translated to conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Concurrent update conflict, retry with a fresh read");
        problem.setType(TYPE_CONFLICT);
        problem.setTitle("Conflict");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Map a {@link MissingPriceException} to HTTP 422 Unprocessable Entity. When the
     * exception carries the offending plan and tier identifiers they are attached as
     * properties on the problem detail so clients can surface targeted error messages.
     */
    @ExceptionHandler(MissingPriceException.class)
    public ResponseEntity<ProblemDetail> handleMissingPrice(MissingPriceException ex) {
        log.warn("Missing price for plan={} tier={}: {}", ex.getPlan(), ex.getTier(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(TYPE_MISSING_PRICE);
        problem.setTitle("Missing catalog price");
        if (ex.getPlan() != null) {
            problem.setProperty("plan", ex.getPlan());
        }
        if (ex.getTier() != null) {
            problem.setProperty("tier", ex.getTier());
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    /**
     * Fallback handler for any unhandled exception. The full stack trace is logged at
     * error level for operator triage; the response body intentionally omits internal
     * details to avoid leaking implementation specifics.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        problem.setType(TYPE_INTERNAL);
        problem.setTitle("Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
