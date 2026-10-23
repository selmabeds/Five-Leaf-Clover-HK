
package com.careeats.backend.ord.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order {

    private final Long orderId;
    private final Long customerId;
    private final Long restaurantId;

    private OrderState state;
    private String rejectionReason;

    private final List<OrderLine> orderLines;
    private final List<StateTransitionRecord> transitionHistory;

    public Order(Long orderId, Long customerId,
                 Long restaurantId, List<OrderLine> orderLines) {

        if (orderId == null || customerId == null || restaurantId == null) {
            throw new IllegalArgumentException(
                    "Order, customer and restaurant IDs are required"
            );
        }

        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException(
                    "An order must contain at least one item"
            );
        }

        if (orderLines.contains(null)) {
            throw new IllegalArgumentException(
                    "Order lines cannot contain null"
            );
        }

        this.orderId = orderId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.state = new PlacedState();

        this.orderLines = new ArrayList<>(orderLines);
        this.transitionHistory = new ArrayList<>();

        transitionHistory.add(new StateTransitionRecord(
                UUID.randomUUID().toString(),
                orderId,
                null,
                OrderStatus.PLACED
        ));
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public OrderStatus getStatus() {
        return state.getStatus();
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public List<OrderLine> getOrderLines() {
        return Collections.unmodifiableList(orderLines);
    }

    public List<StateTransitionRecord> getTransitionHistory() {
        return Collections.unmodifiableList(transitionHistory);
    }

    private void changeState(OrderState nextState) {

        StateTransitionRecord record = new StateTransitionRecord(
                UUID.randomUUID().toString(),
                orderId,
                state.getStatus(),
                nextState.getStatus()
        );

        this.state = nextState;
        this.transitionHistory.add(record);
    }

/*6 Transition methods*/
 public void accept() {
    changeState(state.accept());
}

// Package-private: only PreparationQueue can call this (R-ORD-03 FIFO)
void startPreparing() {
    changeState(state.startPreparing());
}

public void markReady() {
    changeState(state.markReady());
}

public void reject(String reason) {
    OrderState nextState = state.reject(reason);
    this.rejectionReason = reason;
    changeState(nextState);
}

public void cancel() {
    changeState(state.cancel());
}

public void cancelByOperator() {
    changeState(state.cancelByOperator());
}

}
