package com.careeats.backend.prc.domain;

import java.math.BigDecimal;

public record FareDecomposition(
        BigDecimal itemSubtotal,
        BigDecimal deliveryFee,
        BigDecimal serviceFee,
        BigDecimal smallOrderSurcharge,
        BigDecimal totalCharge) {
}
