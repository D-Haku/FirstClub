package com.firstclub.membership.controller;

import com.firstclub.membership.controller.dto.ChangeTierRequest;
import com.firstclub.membership.controller.dto.CurrentMembershipResponse;
import com.firstclub.membership.controller.dto.SubscribeRequest;
import com.firstclub.membership.domain.BenefitConfig;
import com.firstclub.membership.domain.BenefitId;
import com.firstclub.membership.repository.BenefitConfigRepository;
import com.firstclub.membership.service.SubscribeCommand;
import com.firstclub.membership.service.SubscriptionService;
import com.firstclub.membership.service.SubscriptionView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * REST controller that exposes the subscription lifecycle endpoints under
 * {@code /api/v1/subscriptions} per the design's "REST API Contract"
 * section.
 *
 * <p>The controller is a thin transport layer: it translates incoming
 * request DTOs into service-layer commands or arguments, delegates all
 * business logic to {@link SubscriptionService}, and projects results
 * back into REST response DTOs. Validation is enforced declaratively via
 * {@link Valid @Valid} on request bodies; failure cases (no active
 * subscription, same-tier change, optimistic-lock loser, missing price,
 * duplicate active subscription) surface from the service layer as
 * domain exceptions and are mapped to HTTP status codes by
 * {@code GlobalExceptionHandler}.
 *
 * <p>The {@code GET /{userId}/current} endpoint enriches the active
 * subscription view with the configured benefit identifiers for the
 * user's tier in deterministic execution order, mirroring the data
 * shape consumed by the catalog UI.
 *
 * <p>Validates: Requirements 2.1, 3.1, 4.1, 5.1, 5.2, 10.4, 10.5.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final BenefitConfigRepository benefitConfigRepository;

    /**
     * Constructs the controller with its service and repository
     * collaborators.
     *
     * @param subscriptionService     application service that owns the
     *                                {@code Subscription} aggregate
     * @param benefitConfigRepository repository used to materialize the
     *                                benefit chain for a tier on the
     *                                {@code current} endpoint
     */
    public SubscriptionController(SubscriptionService subscriptionService,
                                  BenefitConfigRepository benefitConfigRepository) {
        this.subscriptionService = subscriptionService;
        this.benefitConfigRepository = benefitConfigRepository;
    }

    /**
     * Subscribe the user to the requested plan and tier.
     *
     * @param req validated subscribe request body
     * @return 201 Created with the persisted subscription view
     */
    @PostMapping("")
    public ResponseEntity<SubscriptionView> subscribe(@RequestBody @Valid SubscribeRequest req) {
        var cmd = new SubscribeCommand(req.userId(), req.plan(), req.tier(), req.idempotencyKey());
        SubscriptionView view = subscriptionService.subscribe(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    /**
     * Switch the user's active subscription to a different tier.
     *
     * @param userId owning user identifier from the path
     * @param req    validated change-tier request body
     * @return the updated subscription view
     */
    @PostMapping("/{userId}/change-tier")
    public SubscriptionView changeTier(@PathVariable String userId,
                                       @RequestBody @Valid ChangeTierRequest req) {
        return subscriptionService.changeTier(userId, req.targetTier());
    }

    /**
     * Cancel the user's active subscription. The underlying service call
     * is idempotent on already-canceled subscriptions.
     *
     * @param userId owning user identifier from the path
     * @return the canceled subscription view
     */
    @PostMapping("/{userId}/cancel")
    public SubscriptionView cancel(@PathVariable String userId) {
        return subscriptionService.cancel(userId);
    }

    /**
     * Return the user's current active membership together with the
     * configured benefit chain for its tier, or an inactive response
     * when the user has no active subscription.
     *
     * @param userId owning user identifier from the path
     * @return active or inactive {@link CurrentMembershipResponse}
     */
    @GetMapping("/{userId}/current")
    public CurrentMembershipResponse current(@PathVariable String userId) {
        Optional<SubscriptionView> opt = subscriptionService.getCurrent(userId);
        if (opt.isEmpty()) {
            return CurrentMembershipResponse.inactive();
        }
        SubscriptionView v = opt.get();
        List<BenefitId> benefits = benefitConfigRepository.findByTierOrderByExecutionOrderAsc(v.tier()).stream()
                .map(BenefitConfig::getBenefitId)
                .toList();
        return CurrentMembershipResponse.active(v, benefits);
    }
}
