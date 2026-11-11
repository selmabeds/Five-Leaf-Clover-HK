package com.careeats.backend.prc.domain;

import java.time.Instant;
import java.util.Objects;

public record DeliveryConfirmation(Long orderId, Long courierId, Instant placedAt, Instant deliveredAt) {
    public DeliveryConfirmation {
        Objects.requireNonNull(orderId, "Order ID is required");
        Objects.requireNonNull(courierId, "Courier ID is required");
        Objects.requireNonNull(placedAt, "Placement time is required");
        Objects.requireNonNull(deliveredAt, "Delivery time is required");
        if (deliveredAt.isBefore(placedAt)) throw new IllegalArgumentException("Delivery cannot precede placement");
        placedAt = placedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        deliveredAt = deliveredAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
