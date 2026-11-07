package com.careeats.backend.prc.domain;

import com.careeats.backend.ord.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record LedgerEntry(
        Long orderId,
        LedgerEntryType type,
        OrderStatus orderState,
        Long customerId,
        Long restaurantId,
        Long courierId,
        BigDecimal amount,
        Instant orderPlacedAt,
        Instant deliveredAt,
        Instant recordedAt) {

    public LedgerEntry {
        Objects.requireNonNull(orderId, "Order ID is needed");
        Objects.requireNonNull(type, "Ledger entry type is needed");
        Objects.requireNonNull(orderState, "Order state is needed");
        Objects.requireNonNull(customerId, "Customer ID is needed");
        Objects.requireNonNull(restaurantId, "Restaurant ID is needed");
        Objects.requireNonNull(amount, "Amount is needed");
        Objects.requireNonNull(orderPlacedAt, "Order placement time is needed");
        Objects.requireNonNull(recordedAt, "Ledger record time is needed");

        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
}
