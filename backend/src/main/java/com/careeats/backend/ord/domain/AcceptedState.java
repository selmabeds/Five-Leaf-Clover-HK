package com.careeats.backend.ord.domain;

public class AcceptedState implements OrderState {

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.ACCEPTED;
    }

    @Override
    public OrderState startPreparing() {
        return new PreparingState();
    }

    @Override
    public OrderState cancelByOperator() {
        return new CancelledState();
    }
}