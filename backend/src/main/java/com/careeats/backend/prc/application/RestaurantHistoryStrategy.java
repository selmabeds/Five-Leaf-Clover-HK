
package com.careeats.backend.prc.application;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.reg.domain.*;

public final class RestaurantHistoryStrategy implements HistoryVisibilityStrategy {
    @Override public Role role() { return Role.RESTAURANT; }

    @Override public boolean canViewDetails(User user, OrderHistoryRecord order, Long assignedRestaurantId) {
        return order.restaurantId().equals(assignedRestaurantId);
    }

    @Override public OrderHistoryItem historyItem(OrderHistoryRecord order) {
        return FinancialHistoryItemFactory.from(order, false);
    }
}
