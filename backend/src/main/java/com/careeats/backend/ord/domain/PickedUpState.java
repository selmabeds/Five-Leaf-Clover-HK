package com.careeats.backend.ord.domain;

public class PickedUpState implements OrderState {
    @Override
    public OrderStatus getStatus() {
        return OrderStatus.PICKED_UP;
    }

    @Override
    public OrderState deliver() {
        return new DeliveredState();
    }
}