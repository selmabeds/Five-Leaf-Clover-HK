package com.careeats.backend.ord.domain;

public class RejectedState implements OrderState {

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.REJECTED;
    }
}