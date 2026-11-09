package com.careeats.backend.prc.application;

import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.prc.domain.DeliveredOrder;
import com.careeats.backend.prc.domain.FareDecomposition;
import com.careeats.backend.prc.domain.LedgerEntry;
import com.careeats.backend.prc.domain.LedgerEntryType;
import com.careeats.backend.prc.domain.OrderSummary;
import com.careeats.backend.prc.domain.PricingCalculator;
import com.careeats.backend.prc.infrastructure.LedgerRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@org.springframework.transaction.annotation.Transactional
public class PricingService {

    private final PricingCalculator pricingCalculator;
    private final LedgerRepository ledgerRepository;
    private final Clock clock;
    private java.util.Map<Long, Long> restaurantAssignments = java.util.Map.of();

    public PricingService(PricingCalculator calculator, LedgerRepository ledger, Clock clock,
                          java.util.Map<Long, Long> restaurantAssignments) {
        this(calculator, ledger, clock);
        this.restaurantAssignments = java.util.Objects.requireNonNull(restaurantAssignments);
    }

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
    // Record the delivery information, such as the delivery details and charge.
    public OrderSummary recordDeliveredOrder(
            DeliveredOrder deliveredOrder) {

        ledgerRepository.lockForWrite();
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
        ledgerRepository.saveFare(deliveredOrder, fare);

        return new OrderSummary(
                deliveredOrder.orderId(),
                deliveredOrder.customerId(),
                deliveredOrder.restaurantId(),
                OrderStatus.DELIVERED,
                fare,
                deliveredOrder.deliveredAt()
        );
    }

    // Records an operator-authorized refund as a separate ledger entry
    // Since we made the design choice of having the refund be a separate entry with the same order ID, we preserve the original charge instead of overwriting it.
    public LedgerEntry refundDeliveredOrder(
            User operator,
            Long orderId) {

        ledgerRepository.lockForWrite();
        Objects.requireNonNull(operator, "Operator is needed");
        if (operator.getRole() != Role.OPERATOR) {
            throw new SecurityException("Operator account required");
        }
        Objects.requireNonNull(orderId, "Order ID is needed");

        LedgerEntry charge = ledgerRepository.findByOrderId(orderId).stream()
                .filter(entry -> entry.type() == LedgerEntryType.CHARGE)
                .filter(entry -> entry.orderState() == OrderStatus.DELIVERED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "A delivered charge is required before refunding"
                ));

        boolean alreadyRefunded = ledgerRepository.findByOrderId(orderId)
                .stream()
                .anyMatch(entry -> entry.type() == LedgerEntryType.REFUND);
        if (alreadyRefunded) {
            throw new IllegalStateException(
                    "A refund already exists for this order"
            );
        }

        LedgerEntry refund = new LedgerEntry(
                charge.orderId(),
                LedgerEntryType.REFUND,
                OrderStatus.REFUNDED,
                charge.customerId(),
                charge.restaurantId(),
                charge.courierId(),
                charge.amount(),
                charge.orderPlacedAt(),
                charge.deliveredAt(),
                clock.instant()
        );

        ledgerRepository.save(refund);
        return refund;
    }

    // Returns only the ledger entries visible to the user's role
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<LedgerEntry> historyFor(User user) {
        Objects.requireNonNull(user, "User is needed");
        Objects.requireNonNull(user.getId(), "User ID is needed");
        Objects.requireNonNull(user.getRole(), "User role is needed");

        return ledgerRepository.findAll().stream()
                .filter(entry -> canView(user, entry))
                .toList();
    }

    private boolean canView(User user, LedgerEntry entry) {
        return switch (user.getRole()) {
            case CUSTOMER -> user.getId().equals(entry.customerId());
            case RESTAURANT -> entry.restaurantId().equals(restaurantAssignments.get(user.getId()));
            case COURIER -> user.getId().equals(entry.courierId());
            case OPERATOR -> true;
        };
    }

}
