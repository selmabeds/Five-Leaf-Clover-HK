
package com.careeats.backend.ord.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class PreparationQueue {

    private final Long restaurantId;
    private final Queue<Order> orders;

    public PreparationQueue(Long restaurantId) {
        if (restaurantId == null) {
            throw new IllegalArgumentException(
                    "Restaurant ID is required"
            );
        }

        this.restaurantId = restaurantId;
        this.orders = new ArrayDeque<>();
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void addOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException(
                    "Order cannot be null"
            );
        }

        if (!restaurantId.equals(order.getRestaurantId())) {
            throw new IllegalArgumentException(
                    "Order belongs to another restaurant"
            );
        }

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Only accepted orders can enter the preparation queue"
            );
        }

        if (orders.contains(order)) {
            throw new IllegalArgumentException(
                    "Order is already in the queue"
            );
        }

        orders.offer(order);
    }

    public Order getNextOrder() {
        return orders.peek();
    }

    public Order startNextOrder() {
        if (orders.isEmpty()) {
            throw new IllegalStateException(
                    "Preparation queue is empty"
            );
        }

        Order order = orders.peek();

        order.startPreparing();

        orders.poll();

        return order;
    }

    public int getQueueSize() {
        return orders.size();
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }

    public List<Order> getWaitingOrders() {
        return new ArrayList<>(orders);
    }
}

