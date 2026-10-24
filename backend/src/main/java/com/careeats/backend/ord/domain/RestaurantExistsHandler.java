package com.careeats.backend.ord.domain;

public class RestaurantExistsHandler extends BaseOrderPlacementHandler {

    @Override
    public void handle(OrderPlacementRequest request) {
        if (request.getRestaurant() == null) {
            throw new IllegalArgumentException("Restaurant not found");
        }
        super.handle(request);
    }
}