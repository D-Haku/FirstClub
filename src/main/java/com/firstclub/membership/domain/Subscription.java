package com.firstclub.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a user's paid membership.
 *
 * <p>The entity mirrors the {@code subscription} table defined in
 * {@code schema.sql}. A {@link #version} field enables JPA optimistic
 * locking so that concurrent tier-change attempts on the same row
 * surface as {@code OptimisticLockException} for all but one writer.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>Created via {@link #create(String, MembershipPlan, MembershipTier, BigDecimal, Instant)}
 *       in {@link SubscriptionStatus#ACTIVE} state.</li>
 *   <li>Tier may be swapped while ACTIVE via {@link #changeTier(MembershipTier)}.</li>
 *   <li>Cancellation via {@link #cancel(Instant)} is terminal and idempotent.</li>
 *   <li>{@link SubscriptionStatus#EXPIRED} is a read-time projection (see design)
 *       and is never written to this entity directly.</li>
 * </ul>
 *
 * <p>Validates: Requirements 2.1, 2.2, 9.1.
 */
@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", length = 32, nullable = false)
    private MembershipPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 32, nullable = false)
    private MembershipTier tier;

    @Column(name = "price_charged", precision = 19, scale = 4, nullable = false)
    private BigDecimal priceCharged;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private SubscriptionStatus status;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    /** Required by JPA. Not for use by application code. */
    protected Subscription() {
    }

    /**
     * Build a fresh ACTIVE subscription for the given user.
     *
     * <p>The new subscription's {@code id} is a random UUID, {@code startAt}
     * equals {@code now}, and {@code endAt} equals {@code now} plus the plan's
     * duration. {@code canceledAt} is left {@code null}.
     *
     * @param userId owning user identifier
     * @param plan billing-duration dimension of the membership
     * @param tier benefit-level dimension of the membership
     * @param price catalog price charged at subscribe time
     * @param now subscribe instant; supplied by an injected {@code Clock}
     * @return a new ACTIVE Subscription, not yet persisted
     */
    public static Subscription create(
            String userId,
            MembershipPlan plan,
            MembershipTier tier,
            BigDecimal price,
            Instant now) {
        Subscription s = new Subscription();
        s.id = UUID.randomUUID().toString();
        s.userId = userId;
        s.plan = plan;
        s.tier = tier;
        s.priceCharged = price;
        s.status = SubscriptionStatus.ACTIVE;
        s.startAt = now;
        s.endAt = now.plus(Duration.ofDays(plan.durationDays()));
        s.canceledAt = null;
        return s;
    }

    /**
     * Swap the tier on this subscription.
     *
     * <p>The caller is responsible for validating that {@code newTier}
     * differs from the current tier and for persisting the entity so
     * that the {@link Version} field is checked by the database.
     *
     * @param newTier target tier
     */
    public void changeTier(MembershipTier newTier) {
        this.tier = newTier;
    }

    /**
     * Cancel this subscription.
     *
     * <p>Idempotent: if the subscription is already {@link SubscriptionStatus#CANCELED}
     * this is a no-op so that concurrent cancel requests all observe the
     * same terminal state without overwriting {@code canceledAt}.
     *
     * @param now cancellation instant; supplied by an injected {@code Clock}
     */
    public void cancel(Instant now) {
        if (this.status == SubscriptionStatus.CANCELED) {
            return;
        }
        this.status = SubscriptionStatus.CANCELED;
        this.canceledAt = now;
    }

    /**
     * Stamp the idempotency key used to persist this subscription.
     *
     * <p>Visible to the service layer (same package and downstream
     * services in sibling packages) for use during the subscribe flow.
     *
     * @param key idempotency key; may be {@code null}
     */
    public void setIdempotencyKey(String key) {
        this.idempotencyKey = key;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public MembershipPlan getPlan() {
        return plan;
    }

    public MembershipTier getTier() {
        return tier;
    }

    public BigDecimal getPriceCharged() {
        return priceCharged;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public long getVersion() {
        return version;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    // Package-private setters for tests and infrastructure that need
    // to construct subscriptions in non-default states.

    void setId(String id) {
        this.id = id;
    }

    void setUserId(String userId) {
        this.userId = userId;
    }

    void setPlan(MembershipPlan plan) {
        this.plan = plan;
    }

    void setTier(MembershipTier tier) {
        this.tier = tier;
    }

    void setPriceCharged(BigDecimal priceCharged) {
        this.priceCharged = priceCharged;
    }

    void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    void setCanceledAt(Instant canceledAt) {
        this.canceledAt = canceledAt;
    }

    void setVersion(long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Subscription other)) {
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
        return "Subscription{"
                + "id='" + id + '\''
                + ", userId='" + userId + '\''
                + ", plan=" + plan
                + ", tier=" + tier
                + ", status=" + status
                + ", startAt=" + startAt
                + ", endAt=" + endAt
                + ", canceledAt=" + canceledAt
                + ", version=" + version
                + '}';
    }
}
