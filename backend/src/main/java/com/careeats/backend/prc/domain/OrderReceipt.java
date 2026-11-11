package com.careeats.backend.prc.domain;

import com.careeats.backend.ord.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record OrderReceipt(Long orderId, Long customerId, Long restaurantId, String restaurantName,
    OrderStatus status, String rejectionReason, LocalDateTime placedAt, Instant deliveredAt,
    List<ReceiptLine> lines, BigDecimal itemSubtotal, FareDecomposition fare,
    List<LedgerEntry> entries, BigDecimal refundedAmount, BigDecimal netCharge, boolean refundable) {
    public OrderReceipt {
        lines = List.copyOf(lines);
        entries = List.copyOf(entries);
    }
}
