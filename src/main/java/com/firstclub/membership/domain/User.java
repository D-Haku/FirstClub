package com.firstclub.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Persistence-side representation of a FirstClub user.
 *
 * <p>This entity mirrors the {@code users} table in {@code schema.sql}.
 * The {@code id} is assigned by the caller (no auto-generation) so the
 * same value can be used as the foreign key on {@code subscription.user_id}
 * and elsewhere in the system.
 *
 * <p>The {@link #cohort} attribute is the targeting bucket consumed by
 * cohort-based eligibility rules (see {@code CohortCriterion}); it may be
 * {@code null} for users who do not belong to any cohort.
 *
 * <p>Validates: Requirements 6.1, 8.5.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "cohort", length = 64)
    private String cohort;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Required by JPA. Not for use by application code. */
    protected User() {
    }

    /**
     * Build a fully populated user.
     *
     * @param id         caller-assigned user identifier
     * @param email      contact email; may be {@code null}
     * @param cohort     targeting cohort label; may be {@code null}
     * @param createdAt  instant the user was created
     */
    public User(String id, String email, String cohort, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.cohort = cohort;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getCohort() {
        return cohort;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{"
                + "id='" + id + '\''
                + ", email='" + email + '\''
                + ", cohort='" + cohort + '\''
                + ", createdAt=" + createdAt
                + '}';
    }
}
