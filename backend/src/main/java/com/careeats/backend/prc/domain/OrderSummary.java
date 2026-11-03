package com.careeats.backend.prc.domain;

import com.careeats.backend.ord.domain.OrderStatus;

import java.time.Instant;
import java.util.Objects;

public record OrderSummary(
        Long orderId,
        Long customerId,
        Long restaurantId,
        OrderStatus finalState,
        FareDecomposition fare,
        Instant deliveredAt) {

    public OrderSummary {
        Objects.requireNonNull(orderId, "Order ID is needed");
        Objects.requireNonNull(customerId, "Customer ID is needed");
        Objects.requireNonNull(restaurantId, "Restaurant ID is needed");
        Objects.requireNonNull(finalState, "Final state is needed");
        Objects.requireNonNull(fare, "Fare is needed");
        Objects.requireNonNull(deliveredAt, "Delivery time is needed");
    }
}
