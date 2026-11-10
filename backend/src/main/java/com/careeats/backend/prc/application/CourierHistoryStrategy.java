
package com.careeats.backend.prc.application;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.reg.domain.*;

public final class CourierHistoryStrategy implements HistoryVisibilityStrategy {
    @Override public Role role() { return Role.COURIER; }

    @Override public boolean allowsDetails() { return false; }

    @Override public boolean canViewDetails(User user, OrderHistoryRecord order, Long assignedRestaurantId) {
        return false;
    }

    @Override public OrderHistoryItem historyItem(OrderHistoryRecord row) {
        return new OrderHistoryItem.Courier(row.orderId(), row.restaurantId(),
            row.restaurantName(), row.financialState(), row.placedAt(), row.deliveredAt());
    }
}
