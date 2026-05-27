package com.firstclub.membership.benefit;

import com.firstclub.membership.domain.BenefitConfig;
import com.firstclub.membership.domain.BenefitId;
import com.firstclub.membership.domain.MembershipTier;
import com.firstclub.membership.repository.BenefitConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Indexes all {@link Benefit} beans by their {@link BenefitId} at startup and
 * composes the per-tier benefit chain by joining {@code BenefitConfig} rows
 * (ordered by {@code executionOrder}) with their corresponding strategies.
 *
 * <p>Adding a new benefit is purely additive: define a new {@code @Component}
 * implementing {@link Benefit} with a unique {@link BenefitId} and add a
 * {@code BenefitConfig} row for the desired tier. At startup, the registry
 * folds every {@code Benefit} bean into a map keyed by {@code BenefitId};
 * duplicate registrations are rejected with {@link IllegalStateException}.
 *
 * <p>At apply time, callers obtain the deterministic chain for a tier through
 * {@link #benefitsFor(MembershipTier)}. Each entry is an {@link OrderedBenefit}
 * pairing the {@link Benefit} strategy with its originating
 * {@link BenefitConfig} so the strategy can read tier-specific
 * {@code paramsJson}. {@code BenefitConfig} rows that reference a
 * {@link BenefitId} for which no {@link Benefit} bean has been registered are
 * skipped with a warning rather than failing the request.
 *
 * <p>Validates: Requirements 7.1, 7.2.
 */
@Component
public class BenefitRegistry {

    private static final Logger log = LoggerFactory.getLogger(BenefitRegistry.class);

    private final Map<BenefitId, Benefit> byId;
    private final BenefitConfigRepository benefitConfigRepository;

    /**
     * Constructs the registry by indexing every {@link Benefit} bean by its
     * {@link BenefitId}.
     *
     * @param benefits all {@code Benefit} strategy beans discovered by Spring.
     * @param benefitConfigRepository repository used to materialize per-tier
     *     ordered benefit chains at apply time.
     * @throws IllegalStateException if two {@code Benefit} beans declare the
     *     same {@link BenefitId}.
     */
    public BenefitRegistry(List<Benefit> benefits,
                           BenefitConfigRepository benefitConfigRepository) {
        Objects.requireNonNull(benefits, "benefits");
        this.benefitConfigRepository = Objects.requireNonNull(
                benefitConfigRepository, "benefitConfigRepository");
        this.byId = benefits.stream().collect(Collectors.toMap(
                Benefit::id,
                b -> b,
                (a, b) -> {
                    throw new IllegalStateException(
                            "Duplicate Benefit registered for id=" + a.id()
                                    + ": " + a.getClass().getName()
                                    + " vs " + b.getClass().getName());
                }));
    }

    /**
     * Returns the deterministic benefit chain for the supplied tier in
     * {@code executionOrder} ascending. Each element pairs a {@link Benefit}
     * strategy with the {@link BenefitConfig} row that selected it so the
     * caller (the {@code BenefitEngine}) can invoke
     * {@link Benefit#apply(CheckoutContext, BenefitConfig)} with the proper
     * per-tier parameters.
     *
     * <p>{@link BenefitConfig} rows that reference a {@link BenefitId} with no
     * registered {@link Benefit} bean are logged at warning level and
     * silently skipped so a misconfigured row never breaks checkout.
     *
     * @param tier the membership tier whose benefit chain should be returned.
     * @return ordered list of {@link OrderedBenefit} pairs; empty if the tier
     *     has no configured benefits.
     */
    public List<OrderedBenefit> benefitsFor(MembershipTier tier) {
        Objects.requireNonNull(tier, "tier");
        List<BenefitConfig> configs =
                benefitConfigRepository.findByTierOrderByExecutionOrderAsc(tier);
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        List<OrderedBenefit> chain = new ArrayList<>(configs.size());
        for (BenefitConfig config : configs) {
            Benefit benefit = byId.get(config.getBenefitId());
            if (benefit == null) {
                log.warn("No Benefit bean registered for id={} configured on tier={};"
                                + " skipping config row id={}",
                        config.getBenefitId(), tier, config.getId());
                continue;
            }
            chain.add(new OrderedBenefit(benefit, config));
        }
        return chain;
    }

    /**
     * Pairing of a {@link Benefit} strategy with the {@link BenefitConfig}
     * row that selected it for a particular tier. Returned by
     * {@link #benefitsFor(MembershipTier)} so callers can invoke
     * {@link Benefit#apply(CheckoutContext, BenefitConfig)} with the
     * per-tier parameters.
     *
     * @param benefit the registered strategy.
     * @param config the configuration row that scheduled it.
     */
    public record OrderedBenefit(Benefit benefit, BenefitConfig config) {
    }
}
