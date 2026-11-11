package com.careeats.backend.dlv.domain;

public record Assignment(
        long orderId,
        long courierId,
        Status status
) {
    public enum Status {
        OFFERED,
        ACCEPTED,
        STALLED
    }
}