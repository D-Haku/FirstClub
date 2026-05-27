package com.firstclub.membership.benefit.impl;

import com.firstclub.membership.benefit.AppliedBenefit;
import com.firstclub.membership.benefit.Benefit;
import com.firstclub.membership.benefit.CheckoutContext;
import com.firstclub.membership.domain.BenefitConfig;
import com.firstclub.membership.domain.BenefitId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Applies a percentage discount on the {@link CheckoutContext} subtotal,
 * configured per tier through {@link BenefitConfig#getParamsJson()} (e.g.
 * {@code {"rate": 0.05}} for a 5% discount).
 *
 * <p>Failure model:
 * <ul>
 *   <li>If {@code config} or {@code paramsJson} is null or blank or cannot
 *       be parsed as JSON, this strategy throws {@link RuntimeException} so
 *       the engine records a {@code BenefitFailure}.</li>
 *   <li>If the parsed JSON does not contain a {@code "rate"} field, or
 *       the field cannot be coerced to a numeric value, the rate defaults
 *       to zero, producing a no-op discount and a successful applied
 *       record with a zero adjustment.</li>
 * </ul>
 *
 * <p>Monetary results are normalized to scale 4 with
 * {@link RoundingMode#HALF_UP} to keep totals consistent across the chain.
 *
 * <p>Validates: Requirements 7.1, 8.1.
 */
@Component
public class ExtraDiscountBenefit implements Benefit {

    private static final int MONEY_SCALE = 4;

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper Jackson mapper used to parse {@code paramsJson}.
     */
    public ExtraDiscountBenefit(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public BenefitId id() {
        return BenefitId.EXTRA_DISCOUNT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Computes {@code discount = subtotal * rate}, decreases the context's
     * subtotal by that amount, and records the negation as the adjustment.
     */
    @Override
    public AppliedBenefit apply(CheckoutContext ctx, BenefitConfig config) {
        if (config == null) {
            throw new IllegalArgumentException(
                    "ExtraDiscountBenefit requires a BenefitConfig; got null");
        }
        String paramsJson = config.getParamsJson();
        if (paramsJson == null || paramsJson.isBlank()) {
            throw new IllegalArgumentException(
                    "ExtraDiscountBenefit requires non-blank paramsJson on BenefitConfig id="
                            + config.getId());
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(paramsJson);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Malformed paramsJson on BenefitConfig id=" + config.getId()
                            + ": " + e.getMessage(), e);
        }

        BigDecimal rate = extractRate(root);

        BigDecimal subtotal = ctx.getSubtotal().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal discount = subtotal.multiply(rate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal adjustment = discount.negate().setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        ctx.setSubtotal(subtotal.subtract(discount).setScale(MONEY_SCALE, RoundingMode.HALF_UP));

        BigDecimal ratePercent = rate.multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        String description = "Extra discount " + ratePercent.toPlainString() + "% applied";
        return new AppliedBenefit(BenefitId.EXTRA_DISCOUNT, adjustment, description);
    }

    private BigDecimal extractRate(JsonNode root) {
        if (root == null || !root.hasNonNull("rate")) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        JsonNode rateNode = root.get("rate");
        try {
            if (rateNode.isNumber()) {
                return rateNode.decimalValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
            if (rateNode.isTextual()) {
                return new BigDecimal(rateNode.asText().trim())
                        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
        } catch (NumberFormatException ignored) {
            // fall through to default
        }
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
