
package com.careeats.backend.ord.infrastructure;

import com.careeats.backend.ord.domain.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderRepository {

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

    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }

    public boolean existsById(Long orderId) {
        return orders.containsKey(orderId);
    }
}
