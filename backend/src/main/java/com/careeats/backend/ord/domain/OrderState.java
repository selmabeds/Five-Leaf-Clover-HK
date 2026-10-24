package com.careeats.backend.ord.domain;

public interface OrderState {

    OrderStatus getStatus();

    default OrderState accept() {
        throw new IllegalStateException(
            "Cannot accept order in the following status: " + getStatus()
        );
    }

    default OrderState startPreparing() {
        throw new IllegalStateException(
            "Cannot start preparing order in the following status: " + getStatus()
        );
    }

    default OrderState markReady() {
        throw new IllegalStateException(
            "Cannot mark order as ready in the following status: " + getStatus()
        );
    }

    default OrderState reject(String reason) {
        throw new IllegalStateException(
            "Cannot reject order in the following status: " + getStatus()
        );
    }

    default OrderState cancel() {
        throw new IllegalStateException(
            "Cannot cancel order in the following status: " + getStatus()
        );
    }

    default OrderState cancelByOperator() {
        throw new IllegalStateException(
            "Cannot cancel order by operator in the following status: " + getStatus()
        );
    }
}