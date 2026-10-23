package com.careeats.backend.ord.domain;

public class OrderNotEmptyHandler extends BaseOrderPlacementHandler {

    @Override
    public void handle(OrderPlacementRequest request) {
        if (request.getOrderLines() == null || request.getOrderLines().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        for (OrderLine line : request.getOrderLines()) {
            if (line == null) {
                throw new IllegalArgumentException("Order line cannot be null");
            }
        }

        super.handle(request);
    }
}