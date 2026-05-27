package com.firstclub.membership.service;

import com.firstclub.membership.benefit.BenefitEngine;
import com.firstclub.membership.benefit.CartItem;
import com.firstclub.membership.benefit.CheckoutContext;
import com.firstclub.membership.domain.Subscription;
import com.firstclub.membership.exception.ValidationException;
import com.firstclub.membership.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service that integrates membership benefits into the checkout
 * flow.
 *
 * <p>Per the design's "Apply at Checkout" sequence diagram, the service:
 *
 * <ol>
 *   <li>Wraps the entire operation in
 *       {@link IdempotencyService#runOnce(String, String, String, Class, java.util.function.Supplier)}
 *       so that retried checkout calls with the same idempotency key
 *       return the originally-computed result without re-applying
 *       benefits or invoking the engine a second time.</li>
 *   <li>Validates that the supplied {@code userId} resolves to a known
 *       {@code User}, raising {@link ValidationException} otherwise.</li>
 *   <li>Builds a {@link CheckoutContext} from the request payload.</li>
 *   <li>Looks up the user's active {@link Subscription} via
 *       {@link SubscriptionService#getActiveSubscription(String)}; when
 *       present, invokes the {@link BenefitEngine} to fold member-only
 *       benefits into the context. When absent, the cart passes through
 *       unmodified.</li>
 *   <li>Projects the (possibly mutated) context into a
 *       {@link CheckoutResult} returned to the caller.</li>
 * </ol>
 *
 * <p>Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 9.4.
 */
@Service
public class CheckoutIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutIntegrationService.class);

    private final SubscriptionService subscriptionService;
    private final BenefitEngine benefitEngine;
    private final IdempotencyService idempotencyService;
    private final UserRepository userRepository;

    /**
     * Constructs the service with its collaborators.
     *
     * @param subscriptionService source of the user's active subscription
     * @param benefitEngine       chain of benefit strategies
     * @param idempotencyService  enforces at-least-once idempotency on checkout calls
     * @param userRepository      used to validate that {@code userId} resolves to a known user
     */
    public CheckoutIntegrationService(SubscriptionService subscriptionService,
                                      BenefitEngine benefitEngine,
                                      IdempotencyService idempotencyService,
                                      UserRepository userRepository) {
        this.subscriptionService = subscriptionService;
        this.benefitEngine = benefitEngine;
        this.idempotencyService = idempotencyService;
        this.userRepository = userRepository;
    }

    /**
     * Apply the user's membership benefits to the supplied cart and return
     * the resulting totals and audit trail.
     *
     * <p>The call is idempotent on {@code (userId, idempotencyKey, "checkout")}:
     * a repeated request with the same triplet returns the cached
     * {@link CheckoutResult} without re-running the benefit chain.
     *
     * <p>Failure modes:
     * <ul>
     *   <li>{@link ValidationException} when {@code cmd.userId()} does not
     *       resolve to a known user.</li>
     * </ul>
     *
     * @param cmd checkout command; must not be {@code null}
     * @return the post-benefit checkout result
     */
    @Transactional
    public CheckoutResult applyMembership(CheckoutCommand cmd) {
        return idempotencyService.runOnce(
                cmd.userId(),
                cmd.idempotencyKey(),
                "checkout",
                CheckoutResult.class,
                () -> doApply(cmd));
    }

    private CheckoutResult doApply(CheckoutCommand cmd) {
        if (!userRepository.existsById(cmd.userId())) {
            throw new ValidationException("Unknown user: " + cmd.userId());
        }

        List<CartItem> cartItems = cmd.items().stream()
                .map(i -> CartItem.of(i.sku(), i.qty(), i.unitPrice()))
                .toList();
        CheckoutContext ctx = new CheckoutContext(
                cmd.userId(), cartItems, cmd.subtotal(), cmd.deliveryFee());

        Optional<Subscription> active = subscriptionService.getActiveSubscription(cmd.userId());
        if (active.isPresent()) {
            log.debug("Applying benefits for user={} tier={}",
                    cmd.userId(), active.get().getTier());
            benefitEngine.apply(ctx, active.get().getTier());
        } else {
            log.debug("No active subscription for user={}; cart passes through", cmd.userId());
        }

        return CheckoutResult.from(ctx);
    }
}
