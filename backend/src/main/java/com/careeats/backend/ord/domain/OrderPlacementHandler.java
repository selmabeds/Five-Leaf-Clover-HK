package com.careeats.backend.ord.domain;

/* Defines what every handler can do: be linked to a next handler, and handle a request. */
// A link in the order placement validation chain (Chain of Responsibility, R-ORD-02)
public interface OrderPlacementHandler {

    // Links the next handler and returns it, so links can be chained:
    // a.setNext(b).setNext(c)
    OrderPlacementHandler setNext(OrderPlacementHandler next);

    // Checks the request; throws if invalid, otherwise passes it on
    void handle(OrderPlacementRequest request);
}