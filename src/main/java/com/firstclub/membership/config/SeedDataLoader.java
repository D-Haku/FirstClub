package com.firstclub.membership.config;

import static com.firstclub.membership.domain.BenefitId.EXCLUSIVE_DEALS;
import static com.firstclub.membership.domain.BenefitId.EXTRA_DISCOUNT;
import static com.firstclub.membership.domain.BenefitId.FREE_DELIVERY;
import static com.firstclub.membership.domain.BenefitId.PRIORITY_SUPPORT;
import static com.firstclub.membership.domain.MembershipPlan.MONTHLY;
import static com.firstclub.membership.domain.MembershipPlan.QUARTERLY;
import static com.firstclub.membership.domain.MembershipPlan.YEARLY;
import static com.firstclub.membership.domain.MembershipTier.GOLD;
import static com.firstclub.membership.domain.MembershipTier.PLATINUM;
import static com.firstclub.membership.domain.MembershipTier.SILVER;

import com.firstclub.membership.domain.BenefitConfig;
import com.firstclub.membership.domain.Order;
import com.firstclub.membership.domain.PlanTierPrice;
import com.firstclub.membership.domain.User;
import com.firstclub.membership.repository.BenefitConfigRepository;
import com.firstclub.membership.repository.OrderRepository;
import com.firstclub.membership.repository.PlanTierPriceRepository;
import com.firstclub.membership.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent demo seed loader that populates the catalog and a few
 * representative users + orders so the runnable demo can exercise every
 * endpoint without manual setup.
 *
 * <p>Active only on the {@code demo} profile (the default per
 * {@code application.yml}). The loader is idempotent: if any
 * {@code PlanTierPrice} rows already exist (for example, after a
 * persistent-mode H2 restart) the loader logs and returns early.
 *
 * <p>Validates: Requirements 10.2, 10.3.
 */
@Component
@Profile("demo")
public class SeedDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);
    private static final String CURRENCY_USD = "USD";

    private final UserRepository userRepo;
    private final PlanTierPriceRepository priceRepo;
    private final BenefitConfigRepository benefitConfigRepo;
    private final OrderRepository orderRepo;
    private final Clock clock;

    public SeedDataLoader(UserRepository userRepo,
                          PlanTierPriceRepository priceRepo,
                          BenefitConfigRepository benefitConfigRepo,
                          OrderRepository orderRepo,
                          Clock clock) {
        this.userRepo = userRepo;
        this.priceRepo = priceRepo;
        this.benefitConfigRepo = benefitConfigRepo;
        this.orderRepo = orderRepo;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (priceRepo.count() > 0) {
            log.info("Demo data already seeded; skipping");
            return;
        }
        log.info("Seeding demo data...");
        seedPrices();
        seedBenefitConfigs();
        seedUsers();
        seedOrders();
        log.info("Demo seed complete: prices={}, benefitConfigs={}, users={}, orders={}",
                priceRepo.count(), benefitConfigRepo.count(),
                userRepo.count(), orderRepo.count());
    }

    private void seedPrices() {
        List<PlanTierPrice> prices = List.of(
                new PlanTierPrice(MONTHLY, SILVER, new BigDecimal("4.99"), CURRENCY_USD),
                new PlanTierPrice(MONTHLY, GOLD, new BigDecimal("9.99"), CURRENCY_USD),
                new PlanTierPrice(MONTHLY, PLATINUM, new BigDecimal("14.99"), CURRENCY_USD),
                new PlanTierPrice(QUARTERLY, SILVER, new BigDecimal("12.99"), CURRENCY_USD),
                new PlanTierPrice(QUARTERLY, GOLD, new BigDecimal("26.99"), CURRENCY_USD),
                new PlanTierPrice(QUARTERLY, PLATINUM, new BigDecimal("39.99"), CURRENCY_USD),
                new PlanTierPrice(YEARLY, SILVER, new BigDecimal("49.99"), CURRENCY_USD),
                new PlanTierPrice(YEARLY, GOLD, new BigDecimal("99.99"), CURRENCY_USD),
                new PlanTierPrice(YEARLY, PLATINUM, new BigDecimal("149.99"), CURRENCY_USD));
        priceRepo.saveAll(prices);
    }

    private void seedBenefitConfigs() {
        List<BenefitConfig> configs = List.of(
                new BenefitConfig(SILVER, FREE_DELIVERY, 1, null),

                new BenefitConfig(GOLD, FREE_DELIVERY, 1, null),
                new BenefitConfig(GOLD, EXTRA_DISCOUNT, 2, "{\"rate\":0.05}"),

                new BenefitConfig(PLATINUM, FREE_DELIVERY, 1, null),
                new BenefitConfig(PLATINUM, EXTRA_DISCOUNT, 2, "{\"rate\":0.10}"),
                new BenefitConfig(PLATINUM, EXCLUSIVE_DEALS, 3, null),
                new BenefitConfig(PLATINUM, PRIORITY_SUPPORT, 4, null));
        benefitConfigRepo.saveAll(configs);
    }

    private void seedUsers() {
        List<User> users = List.of(
                new User("u-bronze", "bronze@example.com", "NEW", clock.instant()),
                new User("u-mid", "mid@example.com", "LOYAL", clock.instant()),
                new User("u-vip", "vip@example.com", "EARLY_ADOPTER", clock.instant()));
        userRepo.saveAll(users);
    }

    private void seedOrders() {
        List<Order> orders = new ArrayList<>();
        // Bronze: 2 orders of $10 each placed ~30 days ago — SILVER signal
        orders.add(new Order("o-bronze-1", "u-bronze",
                new BigDecimal("10.00"), clock.instant().minus(Duration.ofDays(30))));
        orders.add(new Order("o-bronze-2", "u-bronze",
                new BigDecimal("10.00"), clock.instant().minus(Duration.ofDays(31))));
        // Mid: 8 orders of $50 placed 35-75 days ago — GOLD signal
        for (int i = 1; i <= 8; i++) {
            orders.add(new Order("o-mid-" + i, "u-mid",
                    new BigDecimal("50.00"),
                    clock.instant().minus(Duration.ofDays(30 + i * 5L))));
        }
        // VIP: 20 orders of $100 placed 3-60 days ago — PLATINUM signal
        for (int i = 1; i <= 20; i++) {
            orders.add(new Order("o-vip-" + i, "u-vip",
                    new BigDecimal("100.00"),
                    clock.instant().minus(Duration.ofDays(i * 3L))));
        }
        orderRepo.saveAll(orders);
    }
}
