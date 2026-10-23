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
    void returnedEventListShouldBeUnmodifiable() {
        OrderEventLogObserver observer = new OrderEventLogObserver();

        assertThrows(
            UnsupportedOperationException.class,
            () -> observer.getEvents().clear()
        );
    }
}
