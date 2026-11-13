
package com.careeats.backend.ord.application;

import com.careeats.backend.ord.domain.ItemAvailableHandler;
import com.careeats.backend.ord.domain.ItemBelongsToRestaurantHandler;
import com.careeats.backend.ord.domain.NoDuplicateItemsHandler;
import com.careeats.backend.ord.domain.Order;
import com.careeats.backend.ord.domain.OrderLine;
import com.careeats.backend.ord.domain.OrderNotEmptyHandler;
import com.careeats.backend.ord.domain.OrderPlacementHandler;
import com.careeats.backend.ord.domain.OrderPlacementRequest;
import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.ord.domain.PreparationQueue;
import com.careeats.backend.ord.domain.Restaurant;
import com.careeats.backend.ord.domain.RestaurantExistsHandler;
import com.careeats.backend.ord.domain.RestaurantOpenHandler;
import com.careeats.backend.ord.domain.OrderEventPublisher;
import com.careeats.backend.ord.domain.OrderEventLogObserver;
import com.careeats.backend.ord.domain.OrderStatusObserver;
import com.careeats.backend.ord.domain.StateTransitionRecord;

import com.careeats.backend.ord.infrastructure.OrderRepository;
import com.careeats.backend.ord.infrastructure.RestaurantRepository;

import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@org.springframework.transaction.annotation.Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepo userRepo;
    private final Runnable dispatchReadyOrders;
    private final OrderPlacementHandler placementChain; //Chain of Responsibility order placement validation (R-ORD-02)
    
    private final OrderEventPublisher eventPublisher =
        new OrderEventPublisher();

    private final OrderEventLogObserver eventLogObserver =
        new OrderEventLogObserver();

    private final OrderStatusObserver statusObserver =
        new OrderStatusObserver();


    // Restaurant user ID -> Restaurant ID
    private final Map<Long, Long> restaurantAssignments;

    public OrderService(
        OrderRepository orderRepository,
        RestaurantRepository restaurantRepository,
        UserRepo userRepo,
        Map<Long, Long> restaurantAssignments
) {
    this(
            orderRepository,
            restaurantRepository,
            userRepo,
            restaurantAssignments,
            () -> {}
    );
}

public OrderService(
        OrderRepository orderRepository,
        RestaurantRepository restaurantRepository,
        UserRepo userRepo,
        Map<Long, Long> restaurantAssignments,
        Runnable dispatchReadyOrders
) {
    if (orderRepository == null
            || restaurantRepository == null
            || userRepo == null
            || restaurantAssignments == null
            || dispatchReadyOrders == null) {
        throw new IllegalArgumentException(
                "OrderService dependencies cannot be null"
        );
    }

    this.orderRepository = orderRepository;
    this.restaurantRepository = restaurantRepository;
    this.userRepo = userRepo;
    this.restaurantAssignments = restaurantAssignments;
    this.dispatchReadyOrders = dispatchReadyOrders;

    eventPublisher.registerObserver(eventLogObserver);
    eventPublisher.registerObserver(statusObserver);

    OrderPlacementHandler firstHandler = new RestaurantExistsHandler();

    firstHandler
            .setNext(new RestaurantOpenHandler())
            .setNext(new OrderNotEmptyHandler())
            .setNext(new NoDuplicateItemsHandler())
            .setNext(new ItemBelongsToRestaurantHandler())
            .setNext(new ItemAvailableHandler());

    this.placementChain = firstHandler;
}
    private void notifyLatestTransition(Order order) {
        orderRepository.save(order);
        List<StateTransitionRecord> history =
            order.getTransitionHistory();

        if (!history.isEmpty()) {
            StateTransitionRecord latestRecord =
                history.get(history.size() - 1);

            if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() {
                            synchronized (OrderService.this) {
                                eventPublisher.notifyObservers(latestRecord);
                            }
                        }
                    });
            } else {
                eventPublisher.notifyObservers(latestRecord);
            }
        }
    }

    // Find the currently authenticated user
    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new SecurityException("User is not authenticated");
        }

        User user = userRepo.findByEmail(
                authentication.getName()
        );

        if (user == null) {
            throw new SecurityException(
                    "Authenticated user not found"
            );
        }

        return user;
    }

    // Require a customer account
    private User requireCustomer() {

        User user = getAuthenticatedUser();

        if (user.getRole() != Role.CUSTOMER) {
            throw new SecurityException(
                    "Customer account required"
            );
        }

        return user;
    }

    // Verify restaurant staff authorization
    private void requireRestaurantStaff(Long restaurantId) {

        User user = getAuthenticatedUser();

        if (user.getRole() != Role.RESTAURANT) {
            throw new SecurityException(
                    "Restaurant account required"
            );
        }

        Long assignedRestaurantId =
                restaurantAssignments.get(user.getId());

        if (assignedRestaurantId == null
                || !assignedRestaurantId.equals(restaurantId)) {

            throw new SecurityException(
                    "User is not authorized for this restaurant"
            );
        }
    }

    private Order requireOrder(Long orderId) {

        if (orderId == null) {
            throw new IllegalArgumentException(
                    "Order ID is required"
            );
        }

        Order order = orderRepository.findById(orderId);

        if (order == null) {
            throw new IllegalArgumentException(
                    "Order not found"
            );
        }

        return order;
    }

    private PreparationQueue getPreparationQueue(Long restaurantId) {

        if (!restaurantRepository.existsById(restaurantId)) {
            throw new IllegalArgumentException(
                    "Restaurant not found"
            );
        }

        PreparationQueue queue = new PreparationQueue(restaurantId);
        orderRepository.findAcceptedForRestaurant(restaurantId).forEach(queue::addOrder);
        return queue;
    }

    // PLACE ORDER
    public synchronized Order placeOrder(
            Long restaurantId,
            List<OrderLine> orderLines
    ) {
        orderRepository.lockForWrite();

        // Permission check stays here: who may place an order
        User customer = requireCustomer();

        Restaurant restaurant =
                restaurantRepository.findById(restaurantId);

        // Validation chain: whether this order is valid (R-ORD-02)
        placementChain.handle(
                new OrderPlacementRequest(restaurantId, restaurant, orderLines)
        );

        Long orderId = orderRepository.nextId();

        while (orderRepository.existsById(orderId)) {
            orderId = orderRepository.nextId();
        }

        Order order = new Order(
                orderId,
                customer.getId(),
                restaurantId,
                new ArrayList<>(orderLines)
        );

        notifyLatestTransition(order);

        return order;
    }

    // ACCEPT ORDER
    public synchronized Order acceptOrder(Long orderId) {
        orderRepository.lockForWrite();

        Order order = requireOrder(orderId);

        requireRestaurantStaff(order.getRestaurantId());

        PreparationQueue queue =
                getPreparationQueue(order.getRestaurantId());
 
        order.accept();

        queue.addOrder(order);

        notifyLatestTransition(order);

        return order;

    }

    // REJECT ORDER
    public synchronized Order rejectOrder(
            Long orderId,
            String reason
    ) {
        orderRepository.lockForWrite();

        Order order = requireOrder(orderId);

        requireRestaurantStaff(order.getRestaurantId());

        
        order.reject(reason);

        notifyLatestTransition(order);

        return order;

    }

    // START NEXT ORDER IN FIFO SEQUENCE
    public synchronized Order startNextOrder(Long restaurantId) {
        orderRepository.lockForWrite();

        requireRestaurantStaff(restaurantId);

        PreparationQueue queue =
                getPreparationQueue(restaurantId);

        Order nextOrder = queue.getNextOrder();

        if (nextOrder == null) {
            throw new IllegalStateException(
                    "No accepted orders waiting in this restaurant"
            );
        }

        
        queue.startNextOrder();

        notifyLatestTransition(nextOrder);

        return nextOrder;

    }

    // MARK ORDER READY
    public synchronized Order markOrderReady(Long orderId) {
    orderRepository.lockForWrite();

    Order order = requireOrder(orderId);

    requireRestaurantStaff(order.getRestaurantId());

    order.markReady();

    notifyLatestTransition(order);
    dispatchReadyOrders.run();

    return order;
}

    // CANCEL ORDER - CUSTOMER OR OPERATOR
    public synchronized Order cancelOrder(Long orderId) {
        orderRepository.lockForWrite();

        User user = getAuthenticatedUser();
        Order order = requireOrder(orderId);

        if (user.getRole() == Role.CUSTOMER) {

            if (!order.getCustomerId().equals(user.getId())) {
                throw new SecurityException(
                        "Customers can only cancel their own orders"
                );
            }

            // Customers can cancel only PLACED orders
            order.cancel();

        } else if (user.getRole() == Role.OPERATOR) {

            // Make sure an accepted order is in its FIFO queue
            PreparationQueue queue = null;

            if (order.getStatus() == OrderStatus.ACCEPTED) {

                queue = getPreparationQueue(
                        order.getRestaurantId()
                );

                boolean found = false;

                for (Order waiting : queue.getWaitingOrders()) {
                    if (waiting.getOrderId().equals(orderId)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new IllegalStateException(
                            "Accepted order is missing from preparation queue"
                    );
                }
            }

            // Domain checks whether this status can be cancelled
            order.cancelByOperator();

            // Accepted orders must leave the waiting queue
            if (queue != null) {
                queue.removeOrder(orderId);
            }

        } else {

            throw new SecurityException(
                    "Only customers and operators can cancel orders"
            );
        }

        notifyLatestTransition(order);

        return order;
    }

    // RETRIEVE ONE ORDER
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Order getOrder(Long orderId) {

        User user = getAuthenticatedUser();
        Order order = requireOrder(orderId);

        if (user.getRole() == Role.CUSTOMER) {

            if (!order.getCustomerId().equals(user.getId())) {
                throw new SecurityException(
                        "Customers can only view their own orders"
                );
            }

        } else if (user.getRole() == Role.RESTAURANT) {

            requireRestaurantStaff(order.getRestaurantId());

        } else if (user.getRole() != Role.OPERATOR) {

            throw new SecurityException(
                    "Not authorized to view this order"
            );
        }

        return order;
    }

    // RETRIEVE CUSTOMER'S ORDERS
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<Order> getCustomerOrders() {

        User customer = requireCustomer();
        return orderRepository.findByCustomerId(customer.getId());
    }


    // Retrive all order of a restaurant
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<Order> getRestaurantOrders(Long restaurantId) {

        // Step 1: Verify restaurant staff authorization
        requireRestaurantStaff(restaurantId);

        // Step 2: Verify the restaurant exists
        if (restaurantId == null
                || !restaurantRepository.existsById(restaurantId)) {
            throw new IllegalArgumentException(
                    "Restaurant not found"
            );
        }

        return orderRepository.findByRestaurantId(restaurantId);
    }

    // READ MODEL FOR DASH (Spec ORD responsibility: "per-restaurant queue length and order states")
    // queued = ACCEPTED orders waiting in the preparation queue (R-ORD-03, used by R-DASH-03 colours);
    // preparing / ready = the info area's quick stats. Counts only, never order details (Spec Appendix A).
    public record QueueSummary(Long restaurantId, long queued, long preparing, long ready) { }

    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<QueueSummary> getQueueSummary() {

        // Any authenticated user (R-REG-02); read-only: no state transitions, no events
        getAuthenticatedUser();

        List<Order> allOrders = orderRepository.findAll();
        List<QueueSummary> summary = new ArrayList<>();

        for (Restaurant restaurant : restaurantRepository.findAll()) {
            Long restaurantId = restaurant.getRestaurantId();
            long queued = 0;
            long preparing = 0;
            long ready = 0;

            for (Order order : allOrders) {
                if (!restaurantId.equals(order.getRestaurantId())) continue;
                if (order.getStatus() == OrderStatus.ACCEPTED) queued++;
                else if (order.getStatus() == OrderStatus.PREPARING) preparing++;
                else if (order.getStatus() == OrderStatus.READY) ready++;
            }

            summary.add(new QueueSummary(restaurantId, queued, preparing, ready));
        }

        return summary;
    }

}
