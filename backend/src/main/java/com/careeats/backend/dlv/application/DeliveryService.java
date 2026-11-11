package com.careeats.backend.dlv.application;

import com.careeats.backend.dlv.domain.Assignment;
import com.careeats.backend.dlv.domain.Courier;
import com.careeats.backend.dlv.domain.CourierState;
import com.careeats.backend.dlv.infrastructure.DeliveryRepository;
import com.careeats.backend.ord.domain.Order;
import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.ord.infrastructure.OrderRepository;
import com.careeats.backend.prc.application.DeliveryBillingService;
import com.careeats.backend.prc.domain.DeliveryConfirmation;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
public class DeliveryService {
    private final DeliveryRepository deliveries;
    private final OrderRepository orders;
    private final UserRepo users;
    private final DispatchService dispatch;
    private final DeliveryBillingService billing;

    public DeliveryService(
            DeliveryRepository deliveries,
            OrderRepository orders,
            UserRepo users,
            DispatchService dispatch,
            DeliveryBillingService billing
    ) {
        this.deliveries = deliveries;
        this.orders = orders;
        this.users = users;
        this.dispatch = dispatch;
        this.billing = billing;
    }

    public record CourierView(
            Courier courier,
            Assignment assignment,
            OrderStatus orderStatus
    ) {}

    public record DispatchView(
            List<Long> queuedOrderIds,
            List<Assignment> stalledAssignments
    ) {}

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CourierView currentCourier() {
        return courierView(requireRole(Role.COURIER));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DispatchView dispatchView() {
        requireRole(Role.OPERATOR);
        return buildDispatchView();
    }

    @Transactional(readOnly = true)
    public List<Courier> onDutyCouriers() {
        return deliveries.findOnDutyCouriers();
    }

    public CourierView goOnDuty() {
        User user = requireRole(Role.COURIER);
        orders.lockForWrite();

        Courier courier = getCourier(user);
        Assignment assignment =
                deliveries.findAssignmentForCourier(user.getId());

        if (assignment != null
                && assignment.status() == Assignment.Status.STALLED) {
            throw new IllegalStateException(
                    "An operator must release your stalled order first");
        }

        if (deliveries.findCourier(user.getId()) == null) {
            deliveries.createCourier(courier);
        }

        if (courier.state() == CourierState.OFFLINE) {
            deliveries.updateCourierState(user.getId(), CourierState.IDLE);
        }

        dispatch.dispatchQueuedOrders();
        return courierView(user);
    }

    public CourierView goOffDuty() {
        User user = requireRole(Role.COURIER);
        orders.lockForWrite();

        takeOffDuty(user);
        return courierView(user);
    }

    public CourierView declineOffer(long orderId) {
        User user = requireRole(Role.COURIER);
        orders.lockForWrite();

        requireAssignment(user, orderId, Assignment.Status.OFFERED);
        requireState(user, CourierState.IDLE);

        takeOffDuty(user);
        return courierView(user);
    }

    public CourierView acceptOffer(long orderId) {
        User user = requireRole(Role.COURIER);
        orders.lockForWrite();

        requireAssignment(user, orderId, Assignment.Status.OFFERED);
        requireState(user, CourierState.IDLE);
        requireOrderState(requireOrder(orderId), OrderStatus.READY);

        deliveries.updateAssignmentStatus(orderId, Assignment.Status.ACCEPTED);
        deliveries.updateCourierState(user.getId(), CourierState.ASSIGNED);

        return courierView(user);
    }

    public CourierView confirmPickup(long orderId) {
        User user = requireRole(Role.COURIER);
        orders.lockForWrite();

        requireAssignment(user, orderId, Assignment.Status.ACCEPTED);
        requireState(user, CourierState.ASSIGNED);

        Order order = requireOrder(orderId);
        order.pickUp();
        orders.save(order);

        deliveries.updateCourierState(user.getId(), CourierState.CARRYING);
        return courierView(user);
    }

    public CourierView confirmDelivery(long orderId) {
        User user = requireRole(Role.COURIER);
        orders.lockForWrite();

        Order order = requireOrder(orderId);
        DeliveryConfirmation existing =
                deliveries.findConfirmedDelivery(orderId);

        if (existing != null) {
            if (!existing.courierId().equals(user.getId())) {
                throw new SecurityException(
                        "Only the courier who carried this order can confirm it");
            }

            billing.recordConfirmedDelivery(existing);
            return courierView(user);
        }

        requireAssignment(user, orderId, Assignment.Status.ACCEPTED);
        requireState(user, CourierState.CARRYING);

        order.deliver();
        orders.save(order);

        // Read back the persisted timestamps at the database's precision.
        Order saved = requireOrder(orderId);
        DeliveryConfirmation confirmation = new DeliveryConfirmation(
                orderId,
                user.getId(),
                transitionTime(saved, OrderStatus.PLACED),
                transitionTime(saved, OrderStatus.DELIVERED)
        );

        billing.recordConfirmedDelivery(confirmation);

        deliveries.removeAssignment(orderId);
        deliveries.updateCourierState(user.getId(), CourierState.IDLE);
        dispatch.dispatchQueuedOrders();

        return courierView(user);
    }

    public DispatchView reassignStalled(long orderId) {
        requireRole(Role.OPERATOR);
        orders.lockForWrite();

        Assignment assignment = deliveries.findAssignmentForOrder(orderId);
        if (assignment == null
                || assignment.status() != Assignment.Status.STALLED) {
            throw new IllegalStateException(
                    "Only a stalled order can be reassigned");
        }

        Courier courier = deliveries.findCourier(assignment.courierId());
        if (courier == null || courier.state() != CourierState.OFFLINE) {
            throw new IllegalStateException(
                    "The stalled order's courier must be offline");
        }

        requireOrderState(requireOrder(orderId), OrderStatus.READY);

        deliveries.removeAssignment(orderId);
        dispatch.dispatchQueuedOrders();

        return buildDispatchView();
    }

    private void takeOffDuty(User user) {
        Courier courier = getCourier(user);

        if (courier.state() == CourierState.CARRYING) {
            throw new IllegalStateException(
                    "Confirm delivery before going off duty");
        }

        if (courier.state() == CourierState.OFFLINE) {
            return;
        }

        Assignment assignment =
                deliveries.findAssignmentForCourier(user.getId());

        deliveries.updateCourierState(user.getId(), CourierState.OFFLINE);

        if (assignment == null) {
            return;
        }

        if (assignment.status() == Assignment.Status.OFFERED) {
            deliveries.removeAssignment(assignment.orderId());
            dispatch.dispatchQueuedOrders();
        } else if (assignment.status() == Assignment.Status.ACCEPTED) {
            deliveries.updateAssignmentStatus(
                    assignment.orderId(), Assignment.Status.STALLED);
        }
    }

    private CourierView courierView(User user) {
        Courier courier = getCourier(user);
        Assignment assignment =
                deliveries.findAssignmentForCourier(user.getId());

        OrderStatus status = assignment == null
                ? null
                : requireOrder(assignment.orderId()).getStatus();

        return new CourierView(courier, assignment, status);
    }

    private DispatchView buildDispatchView() {
        List<Long> queuedIds = deliveries.findWaitingOrders().stream()
                .map(DeliveryRepository.WaitingOrder::orderId)
                .toList();

        return new DispatchView(queuedIds, deliveries.findStalledAssignments());
    }

    private Courier getCourier(User user) {
        Courier courier = deliveries.findCourier(user.getId());

        if (courier != null) {
            return courier;
        }

        // Reproducible starting positions for the simulator's courier accounts.
        long id = user.getId();
        double latitude = 45.49 + Math.floorMod(id, 9) * 0.005;
        double longitude = -73.61 + Math.floorMod(id, 7) * 0.005;

        return new Courier(id, latitude, longitude, CourierState.OFFLINE);
    }

    private Assignment requireAssignment(
            User user,
            long orderId,
            Assignment.Status status
    ) {
        Assignment assignment =
                deliveries.findAssignmentForCourier(user.getId());

        if (assignment == null
                || assignment.orderId() != orderId
                || assignment.status() != status) {
            throw new IllegalStateException(
                    "This order is not available for that courier action");
        }

        return assignment;
    }

    private void requireState(User user, CourierState expected) {
        if (getCourier(user).state() != expected) {
            throw new IllegalStateException("Courier must be " + expected);
        }
    }

    private void requireOrderState(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new IllegalStateException("Order must be " + expected);
        }
    }

    private Order requireOrder(long orderId) {
        Order order = orders.findById(orderId);

        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        return order;
    }

    private Instant transitionTime(Order order, OrderStatus status) {
        return order.getTransitionHistory().stream()
                .filter(record -> record.getNewStatus() == status)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing order transition: " + status))
                .getTimestamp()
                .toInstant(ZoneOffset.UTC);
    }

    private User requireRole(Role expected) {
        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new SecurityException("Authentication is required");
        }

        User user = users.findByEmail(authentication.getName());

        if (user == null || user.getRole() != expected) {
            throw new SecurityException("This command requires role " + expected);
        }

        return user;
    }
}