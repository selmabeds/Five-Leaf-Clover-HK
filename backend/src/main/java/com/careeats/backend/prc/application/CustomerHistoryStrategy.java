package com.careeats.backend.prc.application;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.reg.domain.*;

public final class CustomerHistoryStrategy implements HistoryVisibilityStrategy {
    @Override public Role role() { return Role.CUSTOMER; }

    @Override public boolean canViewDetails(User user, OrderHistoryRecord order, Long assignedRestaurantId) {
        return user.getId().equals(order.customerId());
    }

    @Override public OrderHistoryItem historyItem(OrderHistoryRecord order) {
        return FinancialHistoryItemFactory.from(order, false);
    }
}