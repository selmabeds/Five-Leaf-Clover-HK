package com.careeats.backend.prc.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record DeliveredOrder(
        Long orderId,
        Long customerId,
        Long restaurantId,
        Long courierId,
        List<PricedOrderLine> orderLines,
        Instant placedAt,
        Instant deliveredAt) {

    public DeliveredOrder {
        Objects.requireNonNull(orderId, "Order ID is needed");
        Objects.requireNonNull(customerId, "Customer ID is needed");
        Objects.requireNonNull(restaurantId, "Restaurant ID is needed");
        Objects.requireNonNull(courierId, "Courier ID is needed");
        Objects.requireNonNull(orderLines, "Order lines are needed");
        Objects.requireNonNull(placedAt, "Order placement time is needed");
        Objects.requireNonNull(deliveredAt, "Delivery time is needed");

        if (orderLines.isEmpty()) {
            throw new IllegalArgumentException(
                    "A delivered order must contain at minimum one line"
            );
        }

        if (deliveredAt.isBefore(placedAt)) {
            throw new IllegalArgumentException(
                    "Delivery time cannot be before placement time logically"
            );
        }
    }
}
