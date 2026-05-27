package com.firstclub.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Per-tier configuration of a single benefit in the {@code BenefitEngine} chain.
 *
 * <p>Each row binds a {@link MembershipTier} to a {@link BenefitId} together
 * with an {@code executionOrder} that determines the deterministic order in
 * which the {@code BenefitRegistry} composes benefits for a given tier.
 * The optional {@code paramsJson} carries strategy-specific parameters
 * (for example {@code {"rate":0.05}} for {@code ExtraDiscountBenefit}),
 * which the concrete {@code Benefit} implementation parses at apply time.
 *
 * <p>Validates: Requirements 1.4, 7.1, 7.2.
 */
@Entity
@Table(name = "benefit_config")
public class BenefitConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private MembershipTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_id", nullable = false)
    private BenefitId benefitId;

    @Column(name = "execution_order", nullable = false)
    private int executionOrder;

    @Column(name = "params_json", length = 2000)
    private String paramsJson;

    /** JPA-required no-arg constructor. */
    protected BenefitConfig() {
    }

    /**
     * Creates a new BenefitConfig row. The {@code id} is assigned by the
     * database on persist.
     *
     * @param tier the membership tier this configuration applies to.
     * @param benefitId the benefit strategy identifier.
     * @param executionOrder relative order within the tier's benefit chain
     *     (lower values run first).
     * @param paramsJson JSON-encoded benefit parameters, or {@code null}
     *     when the benefit takes no parameters.
     */
    public BenefitConfig(MembershipTier tier,
                         BenefitId benefitId,
                         int executionOrder,
                         String paramsJson) {
        this.tier = tier;
        this.benefitId = benefitId;
        this.executionOrder = executionOrder;
        this.paramsJson = paramsJson;
    }

    public Long getId() {
        return id;
    }

    public MembershipTier getTier() {
        return tier;
    }

    public BenefitId getBenefitId() {
        return benefitId;
    }

    public int getExecutionOrder() {
        return executionOrder;
    }

    public String getParamsJson() {
        return paramsJson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BenefitConfig other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
