package com.careeats.backend.ord.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderLifecycleTest {

    private Order newOrder() {
        return new Order(1L, 10L, 100L,
                new ArrayList<>(List.of(new OrderLine(1L, 1))));
    }

    @Test
    void newOrderStartsPlacedWithOneRecord() {
        Order order = newOrder();

        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(1, order.getTransitionHistory().size());
    }

    @Test
    void fullLifecycleReachesReady() {
        Order order = newOrder();

        order.accept();
        order.startPreparing();
        order.markReady();

        assertEquals(OrderStatus.READY, order.getStatus());
        assertEquals(4, order.getTransitionHistory().size());
    }

    @Test
    void cannotSkipFromPlacedToReady() {
        Order order = newOrder();

        assertThrows(IllegalStateException.class, order::markReady);

        // Failed transition changes nothing
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(1, order.getTransitionHistory().size());
    }

    @Test
    void rejectWithoutReasonLeavesOrderPlaced() {
        Order order = newOrder();

        assertThrows(IllegalArgumentException.class, () -> order.reject(" "));

        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertNull(order.getRejectionReason());
    }

    @Test
    void cancelledOrderRejectsEveryTransition() {
        Order order = newOrder();
        order.cancel();

        assertThrows(IllegalStateException.class, order::accept);
        assertThrows(IllegalStateException.class, order::startPreparing);
        assertThrows(IllegalStateException.class, order::markReady);
        assertThrows(IllegalStateException.class, () -> order.reject("late"));
        assertThrows(IllegalStateException.class, order::cancelByOperator);
    }

    @Test
    void everyTransitionGetsUniqueEventId() {
        Order order = newOrder();

        order.accept();
        order.startPreparing();
        order.markReady();

        List<StateTransitionRecord> history = order.getTransitionHistory();

        long uniqueIds = history.stream()
                .map(StateTransitionRecord::getEventId)
                .distinct()
                .count();

        assertEquals(history.size(), uniqueIds);
    }

    @Test
    void historyRecordsEachTransitionInOrder() {
        Order order = newOrder();

        order.accept();
        order.startPreparing();

        List<StateTransitionRecord> history = order.getTransitionHistory();

        assertNull(history.get(0).getPreviousStatus());
        assertEquals(OrderStatus.PLACED, history.get(0).getNewStatus());

        assertEquals(OrderStatus.PLACED, history.get(1).getPreviousStatus());
        assertEquals(OrderStatus.ACCEPTED, history.get(1).getNewStatus());

        assertEquals(OrderStatus.ACCEPTED, history.get(2).getPreviousStatus());
        assertEquals(OrderStatus.PREPARING, history.get(2).getNewStatus());
    }
}