package com.firstclub.membership.controller;

import com.firstclub.membership.controller.dto.AppliedBenefitDto;
import com.firstclub.membership.controller.dto.BenefitFailureDto;
import com.firstclub.membership.controller.dto.CheckoutRequest;
import com.firstclub.membership.controller.dto.CheckoutResponse;
import com.firstclub.membership.service.CheckoutIntegrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes the membership checkout integration as
 * {@code POST /api/v1/checkout/apply-membership}.
 *
 * <p>The controller is a thin transport layer: it translates the incoming
 * {@link CheckoutRequest} into a service-layer
 * {@link com.firstclub.membership.service.CheckoutCommand}, delegates to
 * {@link CheckoutIntegrationService#applyMembership}, and projects the
 * returned {@link com.firstclub.membership.service.CheckoutResult} into the
 * REST {@link CheckoutResponse}. All benefit logic, idempotency handling,
 * and validation live in the service layer per the design's "Apply at
 * Checkout" sequence.
 *
 * <p>Validates: Requirements 8.1, 8.4, 10.4, 10.5.
 */
@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CheckoutIntegrationService checkoutService;

    /**
     * Constructs the controller with its single service collaborator.
     *
     * @param checkoutService the application service that applies membership
     *                        benefits to the supplied cart
     */
    public CheckoutController(CheckoutIntegrationService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * Apply the user's membership benefits to the supplied cart and return
     * the post-benefit totals together with audit trails of applied benefits
     * and per-benefit failures.
     *
     * @param req validated checkout request body
     * @return the post-benefit checkout response
     */
    @PostMapping("/apply-membership")
    public CheckoutResponse applyMembership(@RequestBody @Valid CheckoutRequest req) {
        var items = req.cart().items().stream()
                .map(i -> new com.firstclub.membership.service.CheckoutCommand.CartItemView(
                        i.sku(), i.qty(), i.unitPrice()))
                .toList();
        var cmd = new com.firstclub.membership.service.CheckoutCommand(
                req.userId(),
                req.idempotencyKey(),
                items,
                req.cart().subtotal(),
                req.cart().deliveryFee());
        com.firstclub.membership.service.CheckoutResult result = checkoutService.applyMembership(cmd);
        return new CheckoutResponse(
                result.subtotal(),
                result.deliveryFee(),
                result.totalAdjustment(),
                result.prioritySupport(),
                result.appliedBenefits().stream().map(AppliedBenefitDto::from).toList(),
                result.failures().stream().map(BenefitFailureDto::from).toList());
    }
}
