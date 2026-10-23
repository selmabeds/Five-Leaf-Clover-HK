
package com.careeats.backend.ord.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderEventLogObserver implements OrderObserver {

    private final List<StateTransitionRecord> events = new ArrayList<>();

    @Override
    public void update(StateTransitionRecord record) {
        events.add(record);
    }

    public List<StateTransitionRecord> getEvents() {
        return Collections.unmodifiableList(events);
    }
}
