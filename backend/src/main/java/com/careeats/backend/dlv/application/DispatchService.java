package com.careeats.backend.dlv.application;

import com.careeats.backend.dlv.domain.Courier;
import com.careeats.backend.dlv.infrastructure.DeliveryRepository;
import com.careeats.backend.ord.infrastructure.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class DispatchService {
    private final DeliveryRepository deliveries;
    private final OrderRepository orders;

    public DispatchService(
            DeliveryRepository deliveries,
            OrderRepository orders
    ) {
        this.deliveries = deliveries;
        this.orders = orders;
    }

    public void dispatchQueuedOrders() {
        orders.lockForWrite();

        List<Courier> available =
                new ArrayList<>(deliveries.findAvailableCouriers());

        if (available.isEmpty()) {
            return;
        }

        for (DeliveryRepository.WaitingOrder order
                : deliveries.findWaitingOrders()) {

            Courier nearest = available.stream()
                    .min(Comparator
                            .comparingDouble((Courier courier) ->
                                    Math.hypot(
                                            courier.latitude() - order.latitude(),
                                            courier.longitude() - order.longitude()))
                            .thenComparingLong(Courier::id))
                    .orElseThrow();

            deliveries.createOffer(order.orderId(), nearest.id());
            available.remove(nearest);

            if (available.isEmpty()) {
                return;
            }
        }
    }
}