package com.careeats.backend.ord.domain;

// R-ORD-02: no orders at a paused or closed restaurant
public class RestaurantOpenHandler extends BaseOrderPlacementHandler {

    @Override
    public void handle(OrderPlacementRequest request) {
        if (!request.getRestaurant().isOpen()) {
            throw new IllegalStateException("Restaurant is not accepting orders");
        }
        super.handle(request);
    }
}