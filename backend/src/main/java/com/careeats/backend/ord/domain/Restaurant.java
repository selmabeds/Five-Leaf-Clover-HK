
package com.careeats.backend.ord.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Restaurant {

    private final Long restaurantId;
    private final String name;
    private final double latitude;
    private final double longitude;

    private RestaurantStatus status;

    private final List<MenuItem> menuItems;
    private final List<RestaurantTransitionRecord> transitionHistory;

    public Restaurant(Long restaurantId, String name,
                      double latitude, double longitude,
                      RestaurantStatus initialStatus) {

        if (restaurantId == null) {
            throw new IllegalArgumentException(
                    "Restaurant ID is required"
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Restaurant name is required"
            );
        }

        if (initialStatus == null) {
            throw new IllegalArgumentException(
                    "Restaurant status is required"
            );
        }

        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90 ||
                !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Invalid restaurant coordinates"
            );
        }

        this.restaurantId = restaurantId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = initialStatus;

        this.menuItems = new ArrayList<>();
        this.transitionHistory = new ArrayList<>();
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public RestaurantStatus getStatus() {
        return status;
    }

    public List<MenuItem> getMenuItems() {
        return Collections.unmodifiableList(menuItems);
    }

    public List<RestaurantTransitionRecord> getTransitionHistory() {
        return Collections.unmodifiableList(transitionHistory);
    }

    public boolean isOpen() {
        return status == RestaurantStatus.OPEN;
    }

    public void addMenuItem(MenuItem item) {

        if (item == null) {
            throw new IllegalArgumentException(
                    "Menu item cannot be null"
            );
        }

        for (MenuItem existing : menuItems) {
            if (existing.getItemId().equals(item.getItemId())) {
                throw new IllegalArgumentException(
                        "Menu item already exists in this restaurant"
                );
            }
        }

        menuItems.add(item);
    }

    public void pause() {
        changeStatus(RestaurantStatus.PAUSED);
    }

    public void reopen() {
        changeStatus(RestaurantStatus.OPEN);
    }

    private void changeStatus(RestaurantStatus newStatus) {

        if (status == newStatus) {
            throw new IllegalStateException(
                    "Restaurant is already " + newStatus
            );
        }

        RestaurantStatus previousStatus = this.status;

        RestaurantTransitionRecord record =
                new RestaurantTransitionRecord(
                        UUID.randomUUID().toString(),
                        restaurantId,
                        previousStatus,
                        newStatus
                );

        this.status = newStatus;
        this.transitionHistory.add(record);
    }
}

