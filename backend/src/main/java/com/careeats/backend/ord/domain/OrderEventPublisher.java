
package com.careeats.backend.ord.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrderEventPublisher {

    private final List<OrderObserver> observers = new ArrayList<>();

    public void registerObserver(OrderObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");

        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(OrderObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(StateTransitionRecord record) {
        Objects.requireNonNull(record, "Transition record cannot be null");

        for (OrderObserver observer : new ArrayList<>(observers)) {
            observer.update(record);
        }
    }
}
