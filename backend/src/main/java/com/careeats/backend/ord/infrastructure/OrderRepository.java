
package com.careeats.backend.ord.infrastructure;

import com.careeats.backend.ord.domain.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderRepository {

    private final java.util.concurrent.atomic.AtomicLong ids = new java.util.concurrent.atomic.AtomicLong(1);
    public long nextId() { return ids.getAndIncrement(); }
    public void lockForWrite() { }
    public List<Order> findAcceptedForRestaurant(Long restaurantId) {
        return findAll().stream()
            .filter(o -> o.getRestaurantId().equals(restaurantId)
                && o.getStatus() == com.careeats.backend.ord.domain.OrderStatus.ACCEPTED)
            .sorted(java.util.Comparator.comparing(o -> o.getTransitionHistory().get(o.getTransitionHistory().size()-1).getTimestamp()))
            .toList();
    }

    private final Map<Long, Order> orders = new HashMap<>();

    public void save(Order order) {

        if (order == null) {
            throw new IllegalArgumentException(
                    "Order cannot be null"
            );
        }

        orders.put(order.getOrderId(), order);
    }

    public Order findById(Long orderId) {
        return orders.get(orderId);
    }

    public List<Order> findByCustomerId(Long customerId) {
        return findAll().stream().filter(order -> order.getCustomerId().equals(customerId)).toList();
    }

    public List<Order> findByRestaurantId(Long restaurantId) {
        return findAll().stream().filter(order -> order.getRestaurantId().equals(restaurantId)).toList();
    }

    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }

    public boolean existsById(Long orderId) {
        return orders.containsKey(orderId);
    }
}
