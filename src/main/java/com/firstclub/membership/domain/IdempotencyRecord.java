package com.firstclub.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity that persists the result of an idempotent operation so that
 * retried client requests bearing the same {@code (userId, key, operation)}
 * triplet receive the original response instead of executing again.
 *
 * <p>The composite primary key is defined via {@link IdClass} pointing at
 * {@link IdempotencyId}; each {@code @Id} field below has a matching field
 * with the same name and type on {@code IdempotencyId} as required by the
 * JPA specification.
 *
 * <p>The persisted column for {@link #key} is named {@code idem_key} because
 * {@code KEY} is a reserved word in several SQL dialects; the Java field
 * stays {@code key} to keep the API natural.
 *
 * <p>Validates: Requirements 2.4, 8.3, 9.4.
 */
@Entity
@Table(name = "idempotency_record")
@IdClass(IdempotencyId.class)
public class IdempotencyRecord {

    @Id
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Id
    @Column(name = "idem_key", nullable = false, length = 128)
    private String key;

    @Id
    @Column(name = "operation", nullable = false, length = 64)
    private String operation;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Lob
    @Column(name = "response_json")
    private String responseJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** No-arg constructor required by JPA. */
    protected IdempotencyRecord() {
    }

    /**
     * Creates a fully-populated idempotency record.
     *
     * @param userId       owning user identifier
     * @param key          client-supplied idempotency key
     * @param operation    logical operation name (e.g., "subscribe", "checkout")
     * @param requestHash  hash of the original request payload, used to detect
     *                     conflicting reuse of the same key
     * @param responseJson serialized response body returned by the original call
     * @param createdAt    timestamp at which the record was first written
     */
    public IdempotencyRecord(String userId,
                             String key,
                             String operation,
                             String requestHash,
                             String responseJson,
                             Instant createdAt) {
        this.userId = userId;
        this.key = key;
        this.operation = operation;
        this.requestHash = requestHash;
        this.responseJson = responseJson;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getKey() {
        return key;
    }

    public String getOperation() {
        return operation;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
