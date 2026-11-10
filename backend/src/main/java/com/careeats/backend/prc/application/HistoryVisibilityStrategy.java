
package com.careeats.backend.prc.application;

import com.careeats.backend.prc.domain.OrderHistoryItem;
import com.careeats.backend.prc.domain.OrderHistoryRecord;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;

/** Role-specific access and presentation rules for PRC order history. */
public interface HistoryVisibilityStrategy {
    Role role();

    /** Whether this role can request an individual order's financial details. */
    default boolean allowsDetails() { return true; }

    /** Checks ownership/authorization before reading receipt lines or ledger data. */
    boolean canViewDetails(User user, OrderHistoryRecord order, Long assignedRestaurantId);

    /** Returns only the fields this role is allowed to see in the history list. */
    OrderHistoryItem historyItem(OrderHistoryRecord order);

    /** Only operators may see the refundable action for an eligible charge. */
    default boolean canRefund(OrderHistoryRecord order) { return false; }
}
