package com.careeats.backend.prc.infrastructure;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.reg.domain.User;
import java.util.List;

public interface OrderHistoryRepository {
    List<OrderHistoryRecord> findVisibleTo(User user, Long assignedRestaurantId);
    OrderHistoryRecord findById(Long orderId);
    List<ReceiptLine> findLines(Long orderId);
    boolean isCourier(Long userId);
}
