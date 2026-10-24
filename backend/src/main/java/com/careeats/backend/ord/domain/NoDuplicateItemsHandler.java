package com.careeats.backend.ord.domain;

import java.util.HashSet;
import java.util.Set;

public class NoDuplicateItemsHandler extends BaseOrderPlacementHandler {

    @Override
    public void handle(OrderPlacementRequest request) {
        Set<Long> seenItemIds = new HashSet<>();

        for (OrderLine line : request.getOrderLines()) {
            if (!seenItemIds.add(line.getMenuItemId())) {
                throw new IllegalArgumentException(
                        "Duplicate menu item: " + line.getMenuItemId()
                );
            }
        }

        super.handle(request);
    }
}