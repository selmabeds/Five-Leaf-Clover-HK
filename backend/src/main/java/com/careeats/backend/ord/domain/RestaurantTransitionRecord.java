
package com.careeats.backend.ord.domain;

import java.time.LocalDateTime;

public class RestaurantTransitionRecord {

    private final String eventId;
    private final Long restaurantId;
    private final RestaurantStatus previousStatus;
    private final RestaurantStatus newStatus;
    private final LocalDateTime timestamp;

    public RestaurantTransitionRecord(
            String eventId,
            Long restaurantId,
            RestaurantStatus previousStatus,
            RestaurantStatus newStatus
    ) {

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "Event ID is required"
            );
        }

        if (restaurantId == null ||
                previousStatus == null ||
                newStatus == null) {
            throw new IllegalArgumentException(
                    "Restaurant ID and statuses are required"
            );
        }

        if (previousStatus == newStatus) {
            throw new IllegalArgumentException(
                    "Previous and new status must be different"
            );
        }

        this.eventId = eventId;
        this.restaurantId = restaurantId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.timestamp = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public RestaurantStatus getPreviousStatus() {
        return previousStatus;
    }

    public RestaurantStatus getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

