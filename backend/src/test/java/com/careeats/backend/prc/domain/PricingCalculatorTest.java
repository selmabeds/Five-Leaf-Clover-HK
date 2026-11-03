package com.careeats.backend.prc.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator(
            new PricingRules(
                    new BigDecimal("4.99"),
                    new BigDecimal("2.50"),
                    new BigDecimal("15.00"),
                    new BigDecimal("2.00")
            )
    );

    @Test
    void calculatesSubtotalFeesSurchargeAndTotal() {
        FareDecomposition fare = calculator.calculate(List.of(
                new PricedOrderLine(1L, 1, new BigDecimal("10.00"))
        ));

        assertEquals(new BigDecimal("10.00"), fare.itemSubtotal());
        assertEquals(new BigDecimal("2.00"), fare.smallOrderSurcharge());
        assertEquals(new BigDecimal("19.49"), fare.totalCharge());
    }

    @Test
    void doesNotApplySurchargeAtThreshold() {
        FareDecomposition fare = calculator.calculate(List.of(
                new PricedOrderLine(1L, 1, new BigDecimal("15.00"))
        ));

        assertEquals(BigDecimal.ZERO, fare.smallOrderSurcharge());
        assertEquals(new BigDecimal("22.49"), fare.totalCharge());
    }
}
