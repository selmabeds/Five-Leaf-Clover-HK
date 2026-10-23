
package com.careeats.backend.ord.application;

import com.careeats.backend.ord.domain.MenuItem;
import com.careeats.backend.ord.domain.MenuItemStatus;
import com.careeats.backend.ord.domain.Order;
import com.careeats.backend.ord.domain.OrderLine;
import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.ord.domain.PreparationQueue;
import com.careeats.backend.ord.domain.Restaurant;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepo userRepo;
    
    private final OrderEventPublisher eventPublisher =
        new OrderEventPublisher();

    private final OrderEventLogObserver eventLogObserver =
        new OrderEventLogObserver();

    private final OrderStatusObserver statusObserver =
        new OrderStatusObserver();


    // Restaurant user ID -> Restaurant ID
    private final Map<Long, Long> restaurantAssignments;

    // Restaurant ID -> PreparationQueue
    private final Map<Long, PreparationQueue> preparationQueues =
            new HashMap<>();

    private final AtomicLong nextOrderId = new AtomicLong(1);

    public OrderService(
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            UserRepo userRepo,
            Map<Long, Long> restaurantAssignments
    ) {
        if (orderRepository == null
                || restaurantRepository == null
                || userRepo == null
                || restaurantAssignments == null) {

            throw new IllegalArgumentException(
                    "OrderService dependencies cannot be null"
            );
        }

        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepo = userRepo;
        this.restaurantAssignments =
                new HashMap<>(restaurantAssignments);
        eventPublisher.registerObserver(eventLogObserver);
        eventPublisher.registerObserver(statusObserver);

    }

    private void notifyLatestTransition(Order order) {
        List<StateTransitionRecord> history =
            order.getTransitionHistory();

        if (!history.isEmpty()) {
            StateTransitionRecord latestRecord =
                history.get(history.size() - 1);

        eventPublisher.notifyObservers(latestRecord);
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

        return preparationQueues.computeIfAbsent(
                restaurantId,
                id -> new PreparationQueue(id)
        );
    }

    // PLACE ORDER
    public synchronized Order placeOrder(
            Long restaurantId,
            List<OrderLine> orderLines
    ) {

        User customer = requireCustomer();

        Restaurant restaurant =
                restaurantRepository.findById(restaurantId);

        if (restaurant == null) {
            throw new IllegalArgumentException(
                    "Restaurant not found"
            );
        }

        if (!restaurant.isOpen()) {
            throw new IllegalStateException(
                    "Restaurant is not accepting orders"
            );
        }

        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain at least one item"
            );
        }

        Set<Long> requestedItemIds = new HashSet<>();

        for (OrderLine line : orderLines) {

            if (line == null) {
                throw new IllegalArgumentException(
                        "Order line cannot be null"
                );
            }

            Long itemId = line.getMenuItemId();

            if (!requestedItemIds.add(itemId)) {
                throw new IllegalArgumentException(
                        "Duplicate menu item: " + itemId
                );
            }

            MenuItem matchingItem = null;

            for (MenuItem item : restaurant.getMenuItems()) {

                if (item.getItemId().equals(itemId)) {
                    matchingItem = item;
                    break;
                }
            }

            if (matchingItem == null) {
                throw new IllegalArgumentException(
                        "Menu item does not belong to this restaurant: "
                                + itemId
                );
            }

            if (matchingItem.getStatus()
                    != MenuItemStatus.AVAILABLE) {

                throw new IllegalStateException(
                        "Menu item is sold out: " + itemId
                );
            }
        }

        Long orderId = nextOrderId.getAndIncrement();

        while (orderRepository.existsById(orderId)) {
            orderId = nextOrderId.getAndIncrement();
        }

        Order order = new Order(
                orderId,
                customer.getId(),
                restaurantId,
                new ArrayList<>(orderLines)
        );

       
        orderRepository.save(order);

        notifyLatestTransition(order);

        return order;

    }

    // ACCEPT ORDER
    public synchronized Order acceptOrder(Long orderId) {

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

        Order order = requireOrder(orderId);

        requireRestaurantStaff(order.getRestaurantId());

        
        order.reject(reason);

        notifyLatestTransition(order);

        return order;

    }

    // START NEXT ORDER IN FIFO SEQUENCE
    public synchronized Order startNextOrder(Long restaurantId) {

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

        Order order = requireOrder(orderId);

        requireRestaurantStaff(order.getRestaurantId());

        order.markReady();

        notifyLatestTransition(order);

        return order;
    }

    // CANCEL ORDER - CUSTOMER OR OPERATOR
    public synchronized Order cancelOrder(Long orderId) {

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
    public synchronized Order getOrder(Long orderId) {

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
    public synchronized List<Order> getCustomerOrders() {

        User customer = requireCustomer();
        List<Order> customerOrders = new ArrayList<>();

        for (Order order : orderRepository.findAll()) {

            if (order.getCustomerId().equals(customer.getId())) {
                customerOrders.add(order);
            }
        }

        return customerOrders;
    }


    // Retrive all order of a restaurant
    public synchronized List<Order> getRestaurantOrders(Long restaurantId) {

        // Step 1: Verify restaurant staff authorization
        requireRestaurantStaff(restaurantId);

        // Step 2: Verify the restaurant exists
        if (restaurantId == null
                || !restaurantRepository.existsById(restaurantId)) {
            throw new IllegalArgumentException(
                    "Restaurant not found"
            );
        }

        // Step 3: Create a list for the restaurant's orders
        List<Order> restaurantOrders = new ArrayList<>();

        // Step 4: Search all stored orders
        for (Order order : orderRepository.findAll()) {

            // Include only orders belonging to this restaurant
            if (order.getRestaurantId().equals(restaurantId)) {
                restaurantOrders.add(order);
            }
        }

        // Step 5: Return the matching orders
        return restaurantOrders;
    }

}
