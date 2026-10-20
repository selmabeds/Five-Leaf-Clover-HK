
package com.careeats.backend.ord.domain;

import java.time.LocalDateTime;

public class MenuItemTransitionRecord {

    private final String eventId;
    private final Long menuItemId;
    private final MenuItemStatus previousStatus;
    private final MenuItemStatus newStatus;
    private final LocalDateTime timestamp;

    public MenuItemTransitionRecord(
            String eventId,
            Long menuItemId,
            MenuItemStatus previousStatus,
            MenuItemStatus newStatus
    ) {

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "Event ID is required"
            );
        }

        if (menuItemId == null ||
                previousStatus == null ||
                newStatus == null) {
            throw new IllegalArgumentException(
                    "Menu item ID and statuses are required"
            );
        }

        if (previousStatus == newStatus) {
            throw new IllegalArgumentException(
                    "Previous and new status must be different"
            );
        }

        this.eventId = eventId;
        this.menuItemId = menuItemId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.timestamp = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public MenuItemStatus getPreviousStatus() {
        return previousStatus;
    }

    public MenuItemStatus getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

