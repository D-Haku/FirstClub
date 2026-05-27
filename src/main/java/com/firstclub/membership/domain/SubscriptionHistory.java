package com.firstclub.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Append-only audit row for a {@link Subscription} tier transition.
 *
 * <p>One row is written for every successful tier change, capturing the
 * previous tier, the new tier, when the change happened, and an optional
 * human-readable reason. The {@code id} is database-generated; rows are
 * written via the all-args constructor that does not require it.
 *
 * <p>Validates: Requirements 3.2.
 */
@Entity
@Table(name = "subscription_history")
public class SubscriptionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private String subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_tier", nullable = false)
    private MembershipTier previousTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_tier", nullable = false)
    private MembershipTier newTier;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "reason")
    private String reason;

    /** Required no-args constructor for JPA. */
    protected SubscriptionHistory() {
    }

    /**
     * Creates a new history row. The {@code id} is assigned by the database
     * on persist.
     *
     * @param subscriptionId the owning subscription's id; must not be {@code null}.
     * @param previousTier   the tier before the change; must not be {@code null}.
     * @param newTier        the tier after the change; must not be {@code null}.
     * @param changedAt      the instant at which the change took effect; must not be {@code null}.
     * @param reason         optional human-readable reason; may be {@code null}.
     */
    public SubscriptionHistory(
            String subscriptionId,
            MembershipTier previousTier,
            MembershipTier newTier,
            Instant changedAt,
            String reason) {
        this.subscriptionId = subscriptionId;
        this.previousTier = previousTier;
        this.newTier = newTier;
        this.changedAt = changedAt;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public MembershipTier getPreviousTier() {
        return previousTier;
    }

    public MembershipTier getNewTier() {
        return newTier;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscriptionHistory other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
