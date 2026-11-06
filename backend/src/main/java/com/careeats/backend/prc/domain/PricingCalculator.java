package com.careeats.backend.prc.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public class PricingCalculator {

    private static final int DECIMAL_PLACES_AFTER_POINT = 2;
    private static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;

    private final PricingRules pricingRules;

    public PricingCalculator(PricingRules pricingRules) {
        this.pricingRules = Objects.requireNonNull(
                pricingRules,
                "Rules about pricing are needed"
        );
    }

    // Calculates the subtotal, fees, surcharge, and final order charge
    public FareDecomposition calculate(List<PricedOrderLine> orderLines) {
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one priced order line is needed"
            );
        }

        BigDecimal itemSubtotal = orderLines.stream()
                .map(PricedOrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                // Keeps all money values( currency) at two decimals
                .setScale(DECIMAL_PLACES_AFTER_POINT, CURRENCY_ROUNDING);

        BigDecimal smallOrderSurcharge = itemSubtotal.compareTo(
                pricingRules.smallOrderThreshold()
        ) < 0
                ? pricingRules.smallOrderSurcharge()
                : BigDecimal.ZERO;
        BigDecimal serviceFee = itemSubtotal
                .multiply(pricingRules.serviceFeeRate())
                .setScale(DECIMAL_PLACES_AFTER_POINT, CURRENCY_ROUNDING);

        BigDecimal totalCharge = itemSubtotal
                .add(pricingRules.deliveryFee())
                .add(serviceFee)
                .add(smallOrderSurcharge)
                .setScale(DECIMAL_PLACES_AFTER_POINT, CURRENCY_ROUNDING);

        return new FareDecomposition(
                itemSubtotal,
                pricingRules.deliveryFee(),
                serviceFee,
                smallOrderSurcharge,
                totalCharge
        );
    }
}
