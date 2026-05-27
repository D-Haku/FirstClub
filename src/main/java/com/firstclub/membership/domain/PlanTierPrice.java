package com.firstclub.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Catalog row that prices a single (plan, tier) combination.
 *
 * <p>One row exists per {@link MembershipPlan} × {@link MembershipTier}
 * pair, which is enforced by the {@code ux_plan_tier_price_plan_tier}
 * unique constraint and mirrored in {@code schema.sql}. The
 * {@link #amount} is stored as {@code DECIMAL(19,4)} so it can be
 * compared and totaled without binary floating-point drift.
 *
 * <p>Validates: Requirements 1.1, 1.5, 2.2.
 */
@Entity
@Table(
    name = "plan_tier_price",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_plan_tier_price_plan_tier",
        columnNames = { "plan", "tier" }
    )
)
public class PlanTierPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 32)
    private MembershipPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 32)
    private MembershipTier tier;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Default constructor required by JPA. */
    protected PlanTierPrice() {
        // for JPA only
    }

    /**
     * Creates a new price row for the given (plan, tier) combination.
     * The identifier is assigned by the database on persist.
     *
     * @param plan     the membership plan
     * @param tier     the membership tier
     * @param amount   the catalog amount
     * @param currency the ISO-4217 3-letter currency code
     */
    public PlanTierPrice(MembershipPlan plan,
                         MembershipTier tier,
                         BigDecimal amount,
                         String currency) {
        this.plan = plan;
        this.tier = tier;
        this.amount = amount;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public MembershipPlan getPlan() {
        return plan;
    }

    public MembershipTier getTier() {
        return tier;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanTierPrice other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
