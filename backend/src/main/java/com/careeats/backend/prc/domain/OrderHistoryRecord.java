package com.careeats.backend.prc.domain;

import com.careeats.backend.ord.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record OrderHistoryRecord(Long orderId, Long customerId, Long restaurantId, String restaurantName,OrderStatus orderStatus, String rejectionReason, LocalDateTime placedAt, Long courierId,Instant deliveredAt, BigDecimal totalCharge, Instant chargedAt, BigDecimal refundedAmount, Instant refundedAt) {

    // Since an order can be charged or refunded, this status needs to be displayed for viewing.
    public OrderStatus financialState() {
        if (refundedAt != null) return OrderStatus.REFUNDED;
        return chargedAt != null ? OrderStatus.DELIVERED : orderStatus;
    }
}
