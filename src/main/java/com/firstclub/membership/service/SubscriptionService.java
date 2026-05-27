package com.firstclub.membership.service;

import com.firstclub.membership.domain.MembershipTier;
import com.firstclub.membership.domain.PlanTierPrice;
import com.firstclub.membership.domain.Subscription;
import com.firstclub.membership.domain.SubscriptionHistory;
import com.firstclub.membership.exception.ConflictException;
import com.firstclub.membership.exception.MissingPriceException;
import com.firstclub.membership.exception.NotFoundException;
import com.firstclub.membership.exception.ValidationException;
import com.firstclub.membership.repository.PlanTierPriceRepository;
import com.firstclub.membership.repository.SubscriptionHistoryRepository;
import com.firstclub.membership.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Application service that manages the {@link Subscription} aggregate.
 *
 * <p>The service is the single entry point for the subscribe, change-tier,
 * cancel, and read operations on a user's membership. It coordinates four
 * concurrency-control mechanisms outlined in the design's Concurrency
 * Design section:
 *
 * <ul>
 *   <li><strong>Idempotent subscribe</strong> via {@link IdempotencyService#runOnce}
 *       so retried subscribe requests do not create duplicate rows.</li>
 *   <li><strong>Pre-check + partial unique index</strong> to ensure at most
 *       one ACTIVE subscription per user. Whichever insert loses the race
 *       surfaces as {@link DataIntegrityViolationException} and is
 *       translated into {@link ConflictException}.</li>
 *   <li><strong>Optimistic locking</strong> via {@link Subscription}'s
 *       {@code @Version} field, retried on {@code changeTier} via Spring
 *       Retry's {@link Retryable} annotation. After exhausting retries
 *       the {@link Recover} method translates the failure into a
 *       {@link ConflictException}.</li>
 *   <li><strong>Read-time EXPIRED projection</strong>: the active-lookup
 *       query filters {@code endAt > now} so stale ACTIVE rows that have
 *       passed their end date are not considered active without requiring
 *       a write.</li>
 * </ul>
 *
 * <p>Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 3.1, 3.2, 3.3,
 * 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 9.1, 9.2, 9.3, 9.4.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subRepo;
    private final SubscriptionHistoryRepository historyRepo;
    private final PlanTierPriceRepository priceRepo;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    /**
     * Constructs the service with its collaborators.
     *
     * @param subRepo            repository for {@link Subscription} aggregates
     * @param historyRepo        repository for tier-change audit rows
     * @param priceRepo          repository for catalog prices
     * @param idempotencyService idempotency wrapper used by {@link #subscribe}
     * @param clock              clock used to stamp {@code startAt}, {@code endAt},
     *                           {@code canceledAt}, and history rows
     */
    public SubscriptionService(SubscriptionRepository subRepo,
                               SubscriptionHistoryRepository historyRepo,
                               PlanTierPriceRepository priceRepo,
                               IdempotencyService idempotencyService,
                               Clock clock) {
        this.subRepo = subRepo;
        this.historyRepo = historyRepo;
        this.priceRepo = priceRepo;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    /**
     * Subscribes the user to the requested plan and tier, returning the
     * resulting subscription view.
     *
     * <p>The call is wrapped in
     * {@link IdempotencyService#runOnce(String, String, String, Class, java.util.function.Supplier)}
     * so retried requests with the same idempotency key return the
     * originally-persisted response without creating a second row.
     *
     * <p>Failure modes:
     * <ul>
     *   <li>{@link MissingPriceException} when no {@code PlanTierPrice}
     *       exists for the requested combination.</li>
     *   <li>{@link ConflictException} when the user already has an active
     *       subscription, including the case where two concurrent inserts
     *       race and the partial unique index rejects the loser.</li>
     * </ul>
     *
     * @param cmd subscribe command; must not be {@code null}
     * @return the persisted subscription as a view
     */
    @Transactional
    public SubscriptionView subscribe(SubscribeCommand cmd) {
        return idempotencyService.runOnce(
                cmd.userId(),
                cmd.idempotencyKey(),
                "subscribe",
                SubscriptionView.class,
                () -> doSubscribe(cmd));
    }

    private SubscriptionView doSubscribe(SubscribeCommand cmd) {
        if (cmd.plan() == null) {
            throw new ValidationException("plan must not be null");
        }
        if (cmd.tier() == null) {
            throw new ValidationException("tier must not be null");
        }

        PlanTierPrice price = priceRepo.findByPlanAndTier(cmd.plan(), cmd.tier())
                .orElseThrow(() -> new MissingPriceException(
                        cmd.plan().name(), cmd.tier().name()));

        Optional<Subscription> active = subRepo.findActive(cmd.userId(), clock.instant());
        if (active.isPresent()) {
            throw new ConflictException(
                    "User " + cmd.userId() + " already has an active subscription "
                            + active.get().getId());
        }

        Subscription s = Subscription.create(
                cmd.userId(), cmd.plan(), cmd.tier(), price.getAmount(), clock.instant());
        s.setIdempotencyKey(cmd.idempotencyKey());

        try {
            Subscription saved = subRepo.save(s);
            log.debug("Subscribed user={} plan={} tier={} subscriptionId={}",
                    cmd.userId(), cmd.plan(), cmd.tier(), saved.getId());
            return SubscriptionView.from(saved);
        } catch (DataIntegrityViolationException e) {
            log.debug("Subscribe lost race for user={} plan={} tier={}",
                    cmd.userId(), cmd.plan(), cmd.tier());
            throw new ConflictException(
                    "Active subscription already exists (race)", e);
        }
    }

    /**
     * Switches the user's active subscription to {@code targetTier}.
     *
     * <p>The method is annotated with
     * {@link Retryable @Retryable(retryFor = ObjectOptimisticLockingFailureException.class)}
     * so that a losing writer in a concurrent tier-change race is retried
     * up to three times with exponential backoff (25ms, 50ms). When the
     * underlying row truly cannot be written, {@link #recoverChangeTier}
     * translates the final failure into a {@link ConflictException}.
     *
     * <p>Failure modes:
     * <ul>
     *   <li>{@link NotFoundException} when the user has no active
     *       subscription.</li>
     *   <li>{@link ValidationException} when {@code targetTier} equals the
     *       current tier.</li>
     *   <li>{@link ConflictException} when retries are exhausted.</li>
     * </ul>
     *
     * @param userId     owning user identifier
     * @param targetTier desired tier; must differ from the current tier
     * @return the updated subscription view
     */
    @Transactional
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 25, multiplier = 2.0))
    public SubscriptionView changeTier(String userId, MembershipTier targetTier) {
        Subscription s = subRepo.findActive(userId, clock.instant())
                .orElseThrow(() -> new NotFoundException(
                        "No active subscription for user " + userId));

        if (s.getTier() == targetTier) {
            throw new ValidationException(
                    "Subscription is already on tier " + targetTier);
        }

        MembershipTier previous = s.getTier();
        s.changeTier(targetTier);

        // Flush so the version check happens inside this method's frame
        // and any optimistic-lock failure is observable by @Retryable.
        subRepo.saveAndFlush(s);

        SubscriptionHistory h = new SubscriptionHistory(
                s.getId(), previous, targetTier, clock.instant(), "tier change");
        historyRepo.save(h);

        log.debug("Changed tier user={} subscriptionId={} {} -> {}",
                userId, s.getId(), previous, targetTier);
        return SubscriptionView.from(s);
    }

    /**
     * Recovery path for {@link #changeTier} after retries are exhausted.
     *
     * <p>The signature mirrors the original method's parameters because
     * {@link Recover} requires the recovery method to match the throwable
     * followed by the original argument list.
     *
     * @param ole        the final optimistic-locking failure
     * @param userId     owning user identifier (carried through from the original call)
     * @param targetTier requested target tier (carried through from the original call)
     * @return never returns; always throws {@link ConflictException}
     */
    @Recover
    public SubscriptionView recoverChangeTier(
            ObjectOptimisticLockingFailureException ole,
            String userId,
            MembershipTier targetTier) {
        log.debug("Tier change exhausted retries for user={} target={}",
                userId, targetTier);
        throw new ConflictException(
                "Could not change tier due to concurrent modification", ole);
    }

    /**
     * Cancels the user's active subscription.
     *
     * <p>The underlying entity-level {@link Subscription#cancel(java.time.Instant)}
     * call is idempotent: a subscription that is already CANCELED is left
     * unchanged, ensuring concurrent cancel requests all observe the same
     * terminal state without overwriting the original {@code canceledAt}.
     *
     * @param userId owning user identifier
     * @return the canceled subscription view
     * @throws NotFoundException when the user has no active subscription
     */
    @Transactional
    public SubscriptionView cancel(String userId) {
        Subscription s = subRepo.findActive(userId, clock.instant())
                .orElseThrow(() -> new NotFoundException(
                        "No active subscription for user " + userId));
        s.cancel(clock.instant());
        Subscription saved = subRepo.save(s);
        log.debug("Canceled subscriptionId={} user={}", saved.getId(), userId);
        return SubscriptionView.from(saved);
    }

    /**
     * Returns the user's current active subscription as a view, applying
     * the read-time EXPIRED projection (subscriptions whose stored status
     * is ACTIVE but whose {@code endAt} has passed are excluded).
     *
     * @param userId owning user identifier
     * @return the active subscription view, or {@link Optional#empty()} if none
     */
    @Transactional(readOnly = true)
    public Optional<SubscriptionView> getCurrent(String userId) {
        return subRepo.findActive(userId, clock.instant())
                .map(SubscriptionView::from);
    }

    /**
     * Returns the user's current active {@link Subscription} entity.
     *
     * <p>Used by the checkout integration service, which needs the full
     * entity (in particular the tier) when invoking the benefit engine.
     *
     * @param userId owning user identifier
     * @return the active subscription entity, or {@link Optional#empty()} if none
     */
    @Transactional(readOnly = true)
    public Optional<Subscription> getActiveSubscription(String userId) {
        return subRepo.findActive(userId, clock.instant());
    }
}
