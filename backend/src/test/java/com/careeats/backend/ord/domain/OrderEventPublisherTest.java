
package com.careeats.backend.ord.domain;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OrderEventPublisherTest {

    private StateTransitionRecord createRecord() {
        return new StateTransitionRecord(
                UUID.randomUUID().toString(),
                1L,
                OrderStatus.PLACED,
                OrderStatus.ACCEPTED
        );
    }

    @Test
    void registeredObserverReceivesNotification() {
        OrderEventPublisher publisher = new OrderEventPublisher();
        List<StateTransitionRecord> received = new ArrayList<>();

        publisher.registerObserver(received::add);

        StateTransitionRecord record = createRecord();
        publisher.notifyObservers(record);

        assertEquals(1, received.size());
        assertSame(record, received.get(0));
    }

    @Test
    void removedObserverDoesNotReceiveNotification() {
        OrderEventPublisher publisher = new OrderEventPublisher();
        List<StateTransitionRecord> received = new ArrayList<>();

        OrderObserver observer = received::add;

        publisher.registerObserver(observer);
        publisher.removeObserver(observer);

        publisher.notifyObservers(createRecord());

        assertTrue(received.isEmpty());
    }

    @Test
    void duplicateRegistrationDoesNotNotifyTwice() {
        OrderEventPublisher publisher = new OrderEventPublisher();
        List<StateTransitionRecord> received = new ArrayList<>();

        OrderObserver observer = received::add;

        publisher.registerObserver(observer);
        publisher.registerObserver(observer);

        publisher.notifyObservers(createRecord());

        assertEquals(1, received.size());
    }

    @Test
    void multipleObserversReceiveNotification() {
        OrderEventPublisher publisher = new OrderEventPublisher();

        List<StateTransitionRecord> first = new ArrayList<>();
        List<StateTransitionRecord> second = new ArrayList<>();

        publisher.registerObserver(first::add);
        publisher.registerObserver(second::add);

        publisher.notifyObservers(createRecord());

        assertEquals(1, first.size());
        assertEquals(1, second.size());
    }

    @Test
    void nullObserverIsRejected() {
        OrderEventPublisher publisher = new OrderEventPublisher();

        assertThrows(
                NullPointerException.class,
                () -> publisher.registerObserver(null)
        );
    }

    @Test
    void nullTransitionRecordIsRejected() {
        OrderEventPublisher publisher = new OrderEventPublisher();

        assertThrows(
                NullPointerException.class,
                () -> publisher.notifyObservers(null)
        );
    }
}
