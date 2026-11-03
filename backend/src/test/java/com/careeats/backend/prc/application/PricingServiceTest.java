package com.careeats.backend.prc.application;

import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.prc.domain.DeliveredOrder;
import com.careeats.backend.prc.domain.LedgerEntryType;
import com.careeats.backend.prc.domain.PricedOrderLine;
import com.careeats.backend.prc.domain.PricingCalculator;
import com.careeats.backend.prc.domain.PricingRules;
import com.careeats.backend.prc.infrastructure.LedgerRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingServiceTest {

    private final Instant placedAt = Instant.parse("2026-11-02T18:00:00Z");
    private final Instant deliveredAt = Instant.parse("2026-11-02T18:30:00Z");
    private final LedgerRepository ledgerRepository = new LedgerRepository();
    private final PricingService pricingService = new PricingService(
            new PricingCalculator(new PricingRules(
                    new BigDecimal("4.99"),
                    new BigDecimal("2.50"),
                    new BigDecimal("15.00"),
                    new BigDecimal("2.00")
            )),
            ledgerRepository,
            Clock.fixed(
                    Instant.parse("2026-11-02T18:31:00Z"),
                    ZoneOffset.UTC
            )
    );

    @Test
    void recordsDeliveredOrderSummaryAndCharge() {
        var summary = pricingService.recordDeliveredOrder(
                new DeliveredOrder(
                        1L,
                        10L,
                        20L,
                        30L,
                        List.of(new PricedOrderLine(
                                100L,
                                1,
                                new BigDecimal("10.00")
                        )),
                        placedAt,
                        deliveredAt
                )
        );

        assertEquals(OrderStatus.DELIVERED, summary.finalState());
        assertEquals(new BigDecimal("19.49"), summary.fare().totalCharge());
        assertEquals(1, ledgerRepository.findByOrderId(1L).size());
        assertEquals(
                LedgerEntryType.CHARGE,
                ledgerRepository.findByOrderId(1L).get(0).type()
        );
    }

    @Test
    void rejectsDuplicateDeliveryCharge() {
        DeliveredOrder order = new DeliveredOrder(
                1L,
                10L,
                20L,
                30L,
                List.of(new PricedOrderLine(
                        100L,
                        1,
                        new BigDecimal("10.00")
                )),
                placedAt,
                deliveredAt
        );

        pricingService.recordDeliveredOrder(order);

        assertThrows(
                IllegalStateException.class,
                () -> pricingService.recordDeliveredOrder(order)
        );
    }
}
