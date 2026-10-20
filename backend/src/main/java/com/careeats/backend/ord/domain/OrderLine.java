package com.careeats.backend.ord.domain;

public class OrderLine {

    private final Long menuItemId;
    private final int quantity;

    public OrderLine(Long menuItemId, int quantity) {

        if (menuItemId == null) {
            throw new IllegalArgumentException(
                    "Menu item ID is required"
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        this.menuItemId = menuItemId;
        this.quantity = quantity;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
