
package com.careeats.backend.prc.application;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.reg.domain.*;

public final class OperatorHistoryStrategy implements HistoryVisibilityStrategy {
    @Override public Role role() { return Role.OPERATOR; }

    @Override public boolean canViewDetails(User user, OrderHistoryRecord order, Long assignedRestaurantId) {
        return true;
    }

    @Override public boolean canRefund(OrderHistoryRecord order) {
        return order.chargedAt() != null && order.refundedAt() == null;
    }

    @Override public OrderHistoryItem historyItem(OrderHistoryRecord order) {
        return FinancialHistoryItemFactory.from(order, canRefund(order));
    }
}
