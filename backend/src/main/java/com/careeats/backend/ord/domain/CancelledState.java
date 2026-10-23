package com.careeats.backend.ord.domain;

public class CancelledState implements OrderState {

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.CANCELLED;
    }
}