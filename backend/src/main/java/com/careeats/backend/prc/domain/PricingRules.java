package com.careeats.backend.prc.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record PricingRules(
        BigDecimal deliveryFee,
        BigDecimal serviceFeeRate,
        BigDecimal smallOrderThreshold,
        BigDecimal smallOrderSurcharge) {

    public PricingRules {
        requireNonNegative(deliveryFee, "Delivery fee");
        requireNonNegative(serviceFeeRate, "Service-fee rate");
        requireNonNegative(smallOrderThreshold, "Small-order threshold");
        // The surcharge applies when the item subtotal is below the configured threshold.
        requireNonNegative(smallOrderSurcharge, "Small-order surcharge");
    }

    private static void requireNonNegative(BigDecimal value, String name) {
        Objects.requireNonNull(value, name + " is needed");

        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
    }
}
