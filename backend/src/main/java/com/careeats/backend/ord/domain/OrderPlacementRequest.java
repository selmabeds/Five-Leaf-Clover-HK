package com.careeats.backend.ord.domain;

import java.util.List;

/* Carries the data every placement handler needs (Chain of Responsibility, R-ORD-02) */ 
public class OrderPlacementRequest {

    private final Long restaurantId;
    private final Restaurant restaurant;   // may be null: RestaurantExistsHandler checks this
    private final List<OrderLine> orderLines;

    public OrderPlacementRequest(Long restaurantId,
                                 Restaurant restaurant,
                                 List<OrderLine> orderLines) {
        this.restaurantId = restaurantId;
        this.restaurant = restaurant;
        this.orderLines = orderLines;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public List<OrderLine> getOrderLines() {
        return orderLines;
    }
}