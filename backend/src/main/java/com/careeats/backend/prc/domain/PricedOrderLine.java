package com.careeats.backend.prc.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record PricedOrderLine(
        Long menuItemId,
        int quantity,
        BigDecimal unitPrice) {

    public PricedOrderLine {
        Objects.requireNonNull(menuItemId, "Menu item ID is needed");
        Objects.requireNonNull(unitPrice, "Unit price is needed");

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero (<= 0) for a valid order line"
            );
        }

        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "Unit price cannot be negative number"
            );
        }
    }

    // Calculates the total price for the item and quantity
    public BigDecimal lineTotal() {
        return unitPrice
                .multiply(BigDecimal.valueOf(quantity));
    }
}
