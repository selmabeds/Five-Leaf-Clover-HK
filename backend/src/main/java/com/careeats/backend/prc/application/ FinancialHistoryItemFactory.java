
package com.careeats.backend.prc.application;

import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.prc.domain.OrderHistoryItem;
import com.careeats.backend.prc.domain.OrderHistoryRecord;
import java.math.BigDecimal;

/** Shared financial presentation; authorization remains in each strategy. */
final class FinancialHistoryItemFactory {
    private FinancialHistoryItemFactory() { }

    static OrderHistoryItem.Financial from(OrderHistoryRecord row, boolean refundable) {
        BigDecimal total = row.totalCharge();
        if (total == null && (row.orderStatus() == OrderStatus.REJECTED ||
                             row.orderStatus() == OrderStatus.CANCELLED)) {
            total = new BigDecimal("0.00");
        }
        BigDecimal refunded = row.refundedAmount() == null
            ? new BigDecimal("0.00") : row.refundedAmount();
        return new OrderHistoryItem.Financial(
            row.orderId(), row.customerId(), row.restaurantId(), row.restaurantName(),
            row.financialState(), row.placedAt(), row.deliveredAt(), row.chargedAt(),
            row.refundedAt(), total, refunded, row.chargedAt() != null, refundable);
    }
}
