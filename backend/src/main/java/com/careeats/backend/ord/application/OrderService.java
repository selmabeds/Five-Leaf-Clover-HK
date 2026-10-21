
package com.careeats.backend.ord.application;

import com.careeats.backend.ord.domain.MenuItem;
import com.careeats.backend.ord.domain.MenuItemStatus;
import com.careeats.backend.ord.domain.Order;
import com.careeats.backend.ord.domain.OrderLine;
import com.careeats.backend.ord.domain.Restaurant;
import com.careeats.backend.ord.infrastructure.OrderRepository;
import com.careeats.backend.ord.infrastructure.RestaurantRepository;
import com.careeats.backend.reg.Role;
import com.careeats.backend.reg.User;
import com.careeats.backend.reg.UserRepo;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepo userRepo;

    private final AtomicLong nextOrderId = new AtomicLong(1);

    public OrderService(
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            UserRepo userRepo
    ) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepo = userRepo;
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

        String email = authentication.getName();

        User user = userRepo.findByEmail(email);

        if (user == null) {
            throw new SecurityException("Authenticated user not found");
        }

        return user;
    }

    // Verify that the logged-in user is a customer
    private User requireCustomer() {

        User user = getAuthenticatedUser();

        if (user.getRole() != Role.CUSTOMER) {
            throw new SecurityException(
                    "Only customers can place orders"
            );
        }

        return user;
    }

    // Place a new customer order
    public Order placeOrder(
            Long restaurantId,
            List<OrderLine> orderLines
    ) {

        // Step 1: Verify the customer
        User customer = requireCustomer();

        // Step 2: Verify the restaurant
        Restaurant restaurant =
                restaurantRepository.findById(restaurantId);

        if (restaurant == null) {
            throw new IllegalArgumentException(
                    "Restaurant not found"
            );
        }

        // Step 3: Verify the restaurant is open
        if (!restaurant.isOpen()) {
            throw new IllegalStateException(
                    "Restaurant is not accepting orders"
            );
        }

        // Step 4: Verify the order contains items
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain at least one item"
            );
        }

        // Step 5: Check every requested menu item
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
                        "Duplicate menu item in order: " + itemId
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
                        "Item does not belong to this restaurant: " + itemId
                );
            }

            if (matchingItem.getStatus() != MenuItemStatus.AVAILABLE) {
                throw new IllegalStateException(
                        "Menu item is sold out: " + itemId
                );
            }
        }

        // Step 6: Generate a unique order ID
        Long orderId = nextOrderId.getAndIncrement();

        while (orderRepository.existsById(orderId)) {
            orderId = nextOrderId.getAndIncrement();
        }

        // Step 7: Create the order
        Order order = new Order(
                orderId,
                customer.getId(),
                restaurantId,
                orderLines
        );

        // Step 8: Save the order
        orderRepository.save(order);

        return order;
    }
}
