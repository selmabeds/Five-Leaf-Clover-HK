package com.careeats.backend.ord.domain;
import java.time.LocalDateTime;

public class StateTransitionRecord {
    private final String eventId;
    private final Long orderId;
    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;
    private final LocalDateTime timestamp;

    public StateTransitionRecord(
            String eventId,
            Long orderId,
            OrderStatus previousStatus,
            OrderStatus newStatus
    ) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "Event ID is required"
            );
        }

        if (orderId == null || newStatus == null) {
            throw new IllegalArgumentException(
                    "Order ID and statuses are required"
            );
        }

        if (previousStatus == null && newStatus != OrderStatus.PLACED) {
            throw new IllegalArgumentException(
                    "Only the initial placement can have no previous status"
            );
        }

        this.eventId = eventId;
        this.orderId = orderId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.timestamp = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}




