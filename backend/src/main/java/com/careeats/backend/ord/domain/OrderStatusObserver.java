
package com.careeats.backend.ord.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OrderStatusObserver implements OrderObserver {

    private final Map<Long, OrderStatus> orderStatuses =
            new HashMap<>();

    @Override
    public void update(StateTransitionRecord record) {
        orderStatuses.put(
                record.getOrderId(),
                record.getNewStatus()
        );
    }

    public OrderStatus getOrderStatus(Long orderId) {
        return orderStatuses.get(orderId);
    }

    public Map<Long, OrderStatus> getOrderStatuses() {
        return Collections.unmodifiableMap(orderStatuses);
    }
}
