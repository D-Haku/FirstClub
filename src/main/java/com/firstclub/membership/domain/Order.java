package com.firstclub.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Persistent order placed by a user.
 *
 * <p>Used by the tier recommendation subsystem to evaluate trailing
 * order count and monthly spend per user. The Java type is named
 * {@code Order}, but the underlying table is {@code orders} because
 * {@code ORDER} is a SQL reserved word; this matches the schema in
 * {@code schema.sql}.
 *
 * <p>Validates: Requirements 6.1, 6.5.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "total", nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    /** Required by JPA. */
    protected Order() {
    }

    /**
     * Construct a fully-populated {@code Order}.
     *
     * @param id        externally-assigned identifier
     * @param userId    owning user identifier
     * @param total     order total amount
     * @param placedAt  instant the order was placed
     */
    public Order(String id, String userId, BigDecimal total, Instant placedAt) {
        this.id = id;
        this.userId = userId;
        this.total = total;
        this.placedAt = placedAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order other)) {
            return false;
        }
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
