package com.careeats.backend.prc.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator(
            new PricingRules(
                    new BigDecimal("3.99"),
                    new BigDecimal("0.12"),
                    new BigDecimal("20.00"),
                    new BigDecimal("2.99")
            )
    );

    @Test
    void calculatesSubtotalFeesSurchargeAndTotal() {
        FareDecomposition fare = calculator.calculate(List.of(
                new PricedOrderLine(1L, 1, new BigDecimal("10.00"))
        ));

        assertEquals(new BigDecimal("10.00"), fare.itemSubtotal());
        assertEquals(new BigDecimal("1.20"), fare.serviceFee());
        assertEquals(new BigDecimal("2.99"), fare.smallOrderSurcharge());
        assertEquals(new BigDecimal("18.18"), fare.totalCharge());
    }

    @Test
    void doesNotApplySurchargeAtThreshold() {
        FareDecomposition fare = calculator.calculate(List.of(
                new PricedOrderLine(1L, 1, new BigDecimal("20.00"))
        ));

        assertEquals(new BigDecimal("2.40"), fare.serviceFee());
        assertEquals(BigDecimal.ZERO, fare.smallOrderSurcharge());
        assertEquals(new BigDecimal("26.39"), fare.totalCharge());
    }
}
