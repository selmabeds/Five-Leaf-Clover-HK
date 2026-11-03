package com.careeats.backend.prc.application;

import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.prc.domain.DeliveredOrder;
import com.careeats.backend.prc.domain.FareDecomposition;
import com.careeats.backend.prc.domain.LedgerEntry;
import com.careeats.backend.prc.domain.LedgerEntryType;
import com.careeats.backend.prc.domain.OrderSummary;
import com.careeats.backend.prc.domain.PricingCalculator;
import com.careeats.backend.prc.infrastructure.LedgerRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class PricingService {

    private final PricingCalculator pricingCalculator;
    private final LedgerRepository ledgerRepository;
    private final Clock clock;

    public PricingService(
            PricingCalculator pricingCalculator,
            LedgerRepository ledgerRepository,
            Clock clock) {

        this.pricingCalculator = Objects.requireNonNull(
                pricingCalculator,
                "Pricing calculator is needed"
        );
        this.ledgerRepository = Objects.requireNonNull(
                ledgerRepository,
                "Ledger repository is needed"
        );
        this.clock = Objects.requireNonNull(clock, "Clock is needed");
    }

    // Calculates the final fare and records the charge after delivery
    public synchronized OrderSummary recordDeliveredOrder(
            DeliveredOrder deliveredOrder) {

        Objects.requireNonNull(deliveredOrder, "Delivered order is needed");

        if (!ledgerRepository.findByOrderId(deliveredOrder.orderId()).isEmpty()) {
            throw new IllegalStateException(
                    "A ledger entry already exists for this order"
            );
        }

        FareDecomposition fare = pricingCalculator.calculate(
                deliveredOrder.orderLines()
        );
        Instant recordedAt = clock.instant();

        LedgerEntry charge = new LedgerEntry(
                deliveredOrder.orderId(),
                LedgerEntryType.CHARGE,
                OrderStatus.DELIVERED,
                deliveredOrder.customerId(),
                deliveredOrder.restaurantId(),
                deliveredOrder.courierId(),
                fare.totalCharge(),
                deliveredOrder.placedAt(),
                deliveredOrder.deliveredAt(),
                recordedAt
        );

        ledgerRepository.save(charge);

        return new OrderSummary(
                deliveredOrder.orderId(),
                deliveredOrder.customerId(),
                deliveredOrder.restaurantId(),
                OrderStatus.DELIVERED,
                fare,
                deliveredOrder.deliveredAt()
        );
    }

}
