
package com.careeats.backend.ord.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderEventLogObserverTest {

    @Test
    void newObserverShouldHaveNoEvents() {
        OrderEventLogObserver observer = new OrderEventLogObserver();

        assertTrue(observer.getEvents().isEmpty());
    }

    @Test
    void observerShouldRecordEvents() {
        OrderEventLogObserver observer = new OrderEventLogObserver();

        StateTransitionRecord record = new StateTransitionRecord(
                "event-1",
                1L,
                null,
                OrderStatus.PLACED
        );

        observer.update(record);

        assertEquals(1, observer.getEvents().size());
        assertSame(record, observer.getEvents().get(0));
    }

    @Test
    void observerShouldRecordMultipleEvents() {
        OrderEventLogObserver observer = new OrderEventLogObserver();

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

        assertEquals(2, observer.getEvents().size());
        assertSame(first, observer.getEvents().get(0));
        assertSame(second, observer.getEvents().get(1));
    }

    @Test
    void returnedEventListShouldBeUnmodifiable() {
        OrderEventLogObserver observer = new OrderEventLogObserver();

        assertThrows(
                UnsupportedOperationException.class,
                () -> observer.getEvents().clear()
        );
    }
}
