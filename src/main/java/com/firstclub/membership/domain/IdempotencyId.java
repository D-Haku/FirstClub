package com.firstclub.membership.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key class for {@link IdempotencyRecord}.
 *
 * <p>Mirrors the three {@code @Id} fields of {@code IdempotencyRecord}
 * ({@code userId}, {@code key}, {@code operation}) as required by the
 * JPA {@code @IdClass} contract. JPA needs a public no-arg constructor
 * and value-based {@link #equals(Object)}/{@link #hashCode()} so that
 * persistence-context lookups by primary key work correctly.
 *
 * <p>Validates: Requirements 2.4, 8.3, 9.4.
 */
public class IdempotencyId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String key;
    private String operation;

    /** No-arg constructor required by JPA. */
    public IdempotencyId() {
    }

    /**
     * Creates a fully-populated composite id.
     *
     * @param userId    owning user identifier
     * @param key       client-supplied idempotency key
     * @param operation logical operation name (e.g., "subscribe", "checkout")
     */
    public IdempotencyId(String userId, String key, String operation) {
        this.userId = userId;
        this.key = key;
        this.operation = operation;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IdempotencyId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && Objects.equals(key, that.key)
                && Objects.equals(operation, that.operation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, key, operation);
    }
}
