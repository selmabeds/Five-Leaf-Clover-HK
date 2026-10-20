
package com.careeats.backend.ord.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order {

    private final Long orderId;
    private final Long customerId;
    private final Long restaurantId;

    private OrderStatus status;
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
        this.status = OrderStatus.PLACED;

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
        return status;
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

    private void changeStatus(OrderStatus newStatus) {
        OrderStatus previousStatus = this.status;

        String eventId = UUID.randomUUID().toString();

        StateTransitionRecord record = new StateTransitionRecord(
                eventId,
                orderId,
                previousStatus,
                newStatus
        );

        this.status = newStatus;
        this.transitionHistory.add(record);
    }

    public void accept() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    "Only placed orders can be accepted"
            );
        }

        changeStatus(OrderStatus.ACCEPTED);
    }

    public void startPreparing() {
        if (status != OrderStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Only accepted orders can start preparation"
            );
        }

        changeStatus(OrderStatus.PREPARING);
    }

    public void markReady() {
        if (status != OrderStatus.PREPARING) {
            throw new IllegalStateException(
                    "Only preparing orders can be marked ready"
            );
        }

        changeStatus(OrderStatus.READY);
    }

    public void reject(String reason) {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    "Only placed orders can be rejected"
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "A rejection reason is required"
            );
        }

        this.rejectionReason = reason;
        changeStatus(OrderStatus.REJECTED);
    }

    public void cancel() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    "Only placed orders can be cancelled"
            );
        }

        changeStatus(OrderStatus.CANCELLED);
    }
}
