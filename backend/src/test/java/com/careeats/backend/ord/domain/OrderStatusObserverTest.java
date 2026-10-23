
package com.careeats.backend.ord.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusObserverTest {

    @Test
    void newObserverShouldHaveNoStatuses() {
        OrderStatusObserver observer = new OrderStatusObserver();

        assertTrue(observer.getOrderStatuses().isEmpty());
    }

    @Test
    void observerShouldTrackOrderStatus() {
        OrderStatusObserver observer = new OrderStatusObserver();

        StateTransitionRecord record = new StateTransitionRecord(
                "event-1",
                1L,
                null,
                OrderStatus.PLACED
        );

        observer.update(record);

        assertEquals(OrderStatus.PLACED, observer.getOrderStatus(1L));
    }

    @Test
    void observerShouldUpdateExistingOrderStatus() {
        OrderStatusObserver observer = new OrderStatusObserver();

        StateTransitionRecord first = new StateTransitionRecord(
                "event-1",
                1L,
                null,
                OrderStatus.PLACED
        );

        StateTransitionRecord second = new StateTransitionRecord(
                "event-2",
                1L,
                OrderStatus.PLACED,
                OrderStatus.ACCEPTED
        );

        observer.update(first);
        observer.update(second);

        assertEquals(OrderStatus.ACCEPTED, observer.getOrderStatus(1L));
        assertEquals(1, observer.getOrderStatuses().size());
    }

    @Test
    void observerShouldTrackMultipleOrders() {
        OrderStatusObserver observer = new OrderStatusObserver();

        observer.update(new StateTransitionRecord(
                "event-1", 1L, null, OrderStatus.PLACED
        ));

        observer.update(new StateTransitionRecord(
                "event-2", 2L, null, OrderStatus.PLACED
        ));

        assertEquals(2, observer.getOrderStatuses().size());
        assertEquals(OrderStatus.PLACED, observer.getOrderStatus(1L));
        assertEquals(OrderStatus.PLACED, observer.getOrderStatus(2L));
    }

    @Test
    void returnedStatusMapShouldBeUnmodifiable() {
        OrderStatusObserver observer = new OrderStatusObserver();

        assertThrows(
                UnsupportedOperationException.class,
                () -> observer.getOrderStatuses().put(
                        1L, OrderStatus.PLACED
                )
        );
    }
}
