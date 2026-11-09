package com.careeats.backend.prc.application;

import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.prc.domain.DeliveredOrder;
import com.careeats.backend.prc.domain.LedgerEntryType;
import com.careeats.backend.prc.domain.PricedOrderLine;
import com.careeats.backend.prc.domain.PricingCalculator;
import com.careeats.backend.prc.domain.PricingRules;
import com.careeats.backend.prc.infrastructure.LedgerRepository;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
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
                    new BigDecimal("3.99"),
                    new BigDecimal("0.12"),
                    new BigDecimal("20.00"),
                    new BigDecimal("2.99")
            )),
            ledgerRepository,
            Clock.fixed(
                    Instant.parse("2026-11-02T18:31:00Z"),
                    ZoneOffset.UTC
            ),
            java.util.Map.of(20L, 20L, 21L, 21L)
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
        assertEquals(new BigDecimal("18.18"), summary.fare().totalCharge());
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

    @Test
    void recordsOperatorRefundAsSeparateLedgerEntry() {
        DeliveredOrder order = deliveredOrder(1L);
        pricingService.recordDeliveredOrder(order);

        var refund = pricingService.refundDeliveredOrder(
                userWithRole(Role.OPERATOR),
                order.orderId()
        );

        assertEquals(LedgerEntryType.REFUND, refund.type());
        assertEquals(OrderStatus.REFUNDED, refund.orderState());
        assertEquals(new BigDecimal("18.18"), refund.amount());
        assertEquals(2, ledgerRepository.findByOrderId(order.orderId()).size());
    }

    @Test
    void rejectsRefundByNonOperator() {
        DeliveredOrder order = deliveredOrder(2L);
        pricingService.recordDeliveredOrder(order);

        assertThrows(
                SecurityException.class,
                () -> pricingService.refundDeliveredOrder(
                        userWithRole(Role.CUSTOMER),
                        order.orderId()
                )
        );
    }

    @Test
    void rejectsRefundWithoutDeliveredCharge() {
        assertThrows(
                IllegalStateException.class,
                () -> pricingService.refundDeliveredOrder(
                        userWithRole(Role.OPERATOR),
                        999L
                )
        );
    }

    @Test
    void rejectsDuplicateRefund() {
        DeliveredOrder order = deliveredOrder(3L);
        pricingService.recordDeliveredOrder(order);
        pricingService.refundDeliveredOrder(
                userWithRole(Role.OPERATOR),
                order.orderId()
        );

        assertThrows(
                IllegalStateException.class,
                () -> pricingService.refundDeliveredOrder(
                        userWithRole(Role.OPERATOR),
                        order.orderId()
                )
        );
    }

    @Test
    void scopesHistoryByCustomerRestaurantCourierAndOperatorRole() {
        pricingService.recordDeliveredOrder(deliveredOrder(4L));
        pricingService.recordDeliveredOrder(deliveredOrder(
                5L,
                11L,
                21L,
                31L
        ));
        pricingService.refundDeliveredOrder(
                userWithRole(Role.OPERATOR),
                4L
        );

        assertEquals(
                2,
                pricingService.historyFor(user(10L, Role.CUSTOMER)).size()
        );
        assertEquals(
                2,
                pricingService.historyFor(user(20L, Role.RESTAURANT)).size()
        );
        assertEquals(
                2,
                pricingService.historyFor(user(30L, Role.COURIER)).size()
        );
        assertEquals(
                1,
                pricingService.historyFor(user(11L, Role.CUSTOMER)).size()
        );
        assertEquals(
                3,
                pricingService.historyFor(user(1L, Role.OPERATOR)).size()
        );
    }

    @Test
    void historyDoesNotExposeEntriesToAnotherUserWithSameRole() {
        pricingService.recordDeliveredOrder(deliveredOrder(6L));

        assertEquals(
                0,
                pricingService.historyFor(user(999L, Role.CUSTOMER)).size()
        );
    }

    private DeliveredOrder deliveredOrder(Long orderId) {
        return deliveredOrder(orderId, 10L, 20L, 30L);
    }

    private DeliveredOrder deliveredOrder(
            Long orderId,
            Long customerId,
            Long restaurantId,
            Long courierId) {

        return new DeliveredOrder(
                orderId,
                customerId,
                restaurantId,
                courierId,
                List.of(new PricedOrderLine(
                        100L,
                        1,
                        new BigDecimal("10.00")
                )),
                placedAt,
                deliveredAt
        );
    }

    private User userWithRole(Role role) {
        return user(1L, role);
    }

    private User user(Long id, Role role) {
        return new User(id, role.name(), id + "@test.com",
                "password", role);
    }
}
