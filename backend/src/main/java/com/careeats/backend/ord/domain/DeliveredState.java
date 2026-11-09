package com.careeats.backend.ord.domain;

public class DeliveredState implements OrderState {
    @Override
    public OrderStatus getStatus() {
        return OrderStatus.DELIVERED;
    }
}