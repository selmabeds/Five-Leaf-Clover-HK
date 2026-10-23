package com.careeats.backend.ord.domain;

public class PlacedState implements OrderState {

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.PLACED;
    }

    @Override
    public OrderState accept() {
        return new AcceptedState();
    }

    @Override
    public OrderState reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                "A rejection reason is required"
            );
        }

        return new RejectedState();
    }

    @Override
    public OrderState cancel() {
        return new CancelledState();
    }

    @Override
    public OrderState cancelByOperator() {
        return new CancelledState();
    }
}