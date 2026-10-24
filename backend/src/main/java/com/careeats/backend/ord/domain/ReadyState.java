package com.careeats.backend.ord.domain;

// READY has no allowed transitions in Phase 2; pickup will be added with DLV
public class ReadyState implements OrderState {

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.READY;
    }
}