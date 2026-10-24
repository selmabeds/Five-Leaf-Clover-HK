package com.careeats.backend.ord.domain;

public class PreparingState implements OrderState {

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.PREPARING;
    }

    @Override
    public OrderState markReady() {
        return new ReadyState();
    }

    @Override
    public OrderState cancelByOperator() {
        return new CancelledState();
    }
}