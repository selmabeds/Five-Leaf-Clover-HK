
package com.careeats.backend.ord.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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

    // Add accepted orders at the END of the queue
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
                    "Only accepted orders can enter the queue"
            );
        }

        for (Order existing : orders) {

            if (existing.getOrderId().equals(order.getOrderId())) {
                throw new IllegalStateException(
                        "Order is already in the queue"
                );
            }
        }

        orders.offer(order);
    }

    // Look at the FIRST order without removing it
    public Order getNextOrder() {
        return orders.peek();
    }

    // Start preparation for the FIRST waiting order
    public Order startNextOrder() {

        Order nextOrder = orders.peek();

        if (nextOrder == null) {
            throw new IllegalStateException(
                    "No accepted orders waiting"
            );
        }

        // Only the first accepted order changes to PREPARING
        nextOrder.startPreparing();

        // Remove it after the state transition succeeds
        orders.poll();

        return nextOrder;
    }

    // Remove an accepted order cancelled by an operator
    public boolean removeOrder(Long orderId) {

        if (orderId == null) {
            throw new IllegalArgumentException(
                    "Order ID is required"
            );
        }

        return orders.removeIf(
                order -> order.getOrderId().equals(orderId)
        );
    }

    public int getQueueSize() {
        return orders.size();
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }

    public List<Order> getWaitingOrders() {
        return Collections.unmodifiableList(
                new ArrayList<>(orders)
        );
    }
}
