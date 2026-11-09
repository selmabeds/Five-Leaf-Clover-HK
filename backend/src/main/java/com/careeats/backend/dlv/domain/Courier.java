package com.careeats.backend.dlv.domain;

public record Courier(
        long id,
        double latitude,
        double longitude,
        CourierState state
) {
}