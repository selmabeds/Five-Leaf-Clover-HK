package com.careeats.backend.ord.domain;

// Stores the next link and forwards requests along the chain
public abstract class BaseOrderPlacementHandler implements OrderPlacementHandler {

    private OrderPlacementHandler next;

    @Override
    public OrderPlacementHandler setNext(OrderPlacementHandler next) {
        this.next = next;
        return next;
    }

    // Passes the request to the next handler, if there is one.
    // Concrete handlers do their check first, then call super.handle(request).
    @Override
    public void handle(OrderPlacementRequest request) {
        if (next != null) {
            next.handle(request);
        }
    }
}