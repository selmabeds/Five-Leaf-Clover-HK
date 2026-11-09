package com.careeats.backend.prc.application;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.prc.infrastructure.*;
import com.careeats.backend.ord.domain.OrderStatus;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Transactional
public class DeliveryBillingService {
    private final OrderHistoryRepository orders;
    private final LedgerRepository ledger;
    private final PricingService pricing;
    public DeliveryBillingService(OrderHistoryRepository orders,LedgerRepository ledger,PricingService pricing) {
        this.orders=orders; this.ledger=ledger; this.pricing=pricing;
    }
    // The DLV module must confirm that the order's state is DELIVERED before PRC can create its order summary and charge.
    public OrderSummary recordConfirmedDelivery(DeliveryConfirmation confirmation) {
        Objects.requireNonNull(confirmation,"Delivery confirmation is required");
        ledger.lockForWrite();
        OrderHistoryRecord order=orders.findById(confirmation.orderId());
        if(order==null) throw new PrcNotFoundException("Order not found");
        if(order.orderStatus()!=OrderStatus.DELIVERED) throw new IllegalStateException("DLV must persist confirmed delivery before billing");
        if(!orders.isCourier(confirmation.courierId())) throw new IllegalArgumentException("A courier account is required");
        var existing=ledger.findByOrderId(order.orderId()).stream().filter(entry->entry.type()==LedgerEntryType.CHARGE).findFirst();
        if(existing.isPresent()) { // To avoid double charging, retrying the same delivery confirmation returns the existing result rather than another charge.
            LedgerEntry charge=existing.get();
            if(!confirmation.courierId().equals(charge.courierId()) || !confirmation.placedAt().equals(charge.orderPlacedAt())
                || !confirmation.deliveredAt().equals(charge.deliveredAt())) throw new IllegalStateException("Delivery confirmation conflicts with the recorded charge");
            FareDecomposition fare=ledger.findFare(order.orderId());
            if(fare==null) throw new IllegalStateException("Recorded charge is missing its fare snapshot");
            return new OrderSummary(order.orderId(),order.customerId(),order.restaurantId(),OrderStatus.DELIVERED,fare,charge.deliveredAt());
        }
        var lines=orders.findLines(order.orderId()).stream().map(line->new PricedOrderLine(
            line.menuItemId(),line.quantity(),line.unitPrice())).toList();
        return pricing.recordDeliveredOrder(new DeliveredOrder(order.orderId(),order.customerId(),order.restaurantId(),
            confirmation.courierId(),lines,confirmation.placedAt(),confirmation.deliveredAt()));
    }
}
