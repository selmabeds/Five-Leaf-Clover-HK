package com.careeats.backend.prc.domain;

import com.careeats.backend.ord.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public sealed interface OrderHistoryItem {
    Long orderId();
    OrderStatus status();

    record Financial(Long orderId, Long customerId, Long restaurantId, String restaurantName,
        OrderStatus status, LocalDateTime placedAt, Instant deliveredAt, Instant chargedAt,
        Instant refundedAt, BigDecimal totalCharge, BigDecimal refundedAmount,
        boolean receiptAvailable, boolean refundable) implements OrderHistoryItem { }

    record Courier(Long orderId, Long restaurantId, String restaurantName, OrderStatus status,
        LocalDateTime placedAt, Instant deliveredAt) implements OrderHistoryItem { }
}
