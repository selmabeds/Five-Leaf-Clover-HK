package com.careeats.backend.prc.application;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.prc.infrastructure.*;
import com.careeats.backend.ord.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryBillingServiceTest {
    private final OrderHistoryRepository orders=mock(OrderHistoryRepository.class);
    private final LedgerRepository ledger=new LedgerRepository();
    private final Instant placed=Instant.parse("2026-11-01T12:00:00Z");
    private final Instant delivered=placed.plusSeconds(1800);
    private final DeliveryConfirmation confirmation=new DeliveryConfirmation(10L,3L,placed,delivered);
    private DeliveryBillingService service(String fee) {
        var rules=new PricingRules(new BigDecimal(fee),new BigDecimal("0.12"),new BigDecimal("20.00"),new BigDecimal("2.99"));
        return new DeliveryBillingService(orders,ledger,new PricingService(new PricingCalculator(rules),ledger,Clock.systemUTC()));
    }
    private OrderHistoryRecord row(OrderStatus state) {
        return new OrderHistoryRecord(10L,1L,5L,"Restaurant",state,null,LocalDateTime.of(2026,11,1,12,0),null,null,null,null,null,null);
    }
    @Test void buildsPricingInputFromStoredLinesAndUsesSavedFareOnRetry() {
        when(orders.findById(10L)).thenReturn(row(OrderStatus.DELIVERED));
        when(orders.isCourier(3L)).thenReturn(true);
        when(orders.findLines(10L)).thenReturn(List.of(new ReceiptLine(1L,"Saved Meal",2,new BigDecimal("10.00"))));
        var original=service("3.99").recordConfirmedDelivery(confirmation);
        var retry=service("99.99").recordConfirmedDelivery(confirmation);
        assertEquals(new BigDecimal("26.39"),original.fare().totalCharge());
        assertEquals(original,retry);
        assertEquals(1,ledger.findByOrderId(10L).size());
        verify(orders,times(1)).findLines(10L);
        assertThrows(IllegalStateException.class,()->service("3.99").recordConfirmedDelivery(
            new DeliveryConfirmation(10L,3L,placed,delivered.plusSeconds(1))));
    }
    @Test void rejectsUnconfirmedDeliveryAndWrongCourierWithoutCharging() {
        when(orders.findById(10L)).thenReturn(row(OrderStatus.READY));
        assertThrows(IllegalStateException.class,()->service("3.99").recordConfirmedDelivery(confirmation));
        when(orders.findById(10L)).thenReturn(row(OrderStatus.DELIVERED));
        assertThrows(IllegalArgumentException.class,()->service("3.99").recordConfirmedDelivery(confirmation));
        assertTrue(ledger.findAll().isEmpty());
        assertThrows(PrcNotFoundException.class,()->service("3.99").recordConfirmedDelivery(new DeliveryConfirmation(999L,3L,placed,delivered)));
    }
}
