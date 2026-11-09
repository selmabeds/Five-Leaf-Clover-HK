package com.careeats.backend.prc.domain;

import java.math.BigDecimal;

public record ReceiptLine(Long menuItemId, String description, int quantity,BigDecimal unitPrice, BigDecimal lineTotal) {
    public ReceiptLine(Long itemId, String description, int quantity, BigDecimal unitPrice) {
        this(itemId, description, quantity, unitPrice, unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }
}
