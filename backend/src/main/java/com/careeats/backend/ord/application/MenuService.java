
package com.careeats.backend.ord.application;

import com.careeats.backend.ord.domain.MenuItem;
import com.careeats.backend.ord.domain.Restaurant;
import com.careeats.backend.ord.infrastructure.RestaurantRepository;

import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

@org.springframework.transaction.annotation.Transactional
public class MenuService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepo userRepo;

    // Restaurant user ID -> Restaurant ID
    private final Map<Long, Long> restaurantAssignments;

    public MenuService(
            RestaurantRepository restaurantRepository,
            UserRepo userRepo,
            Map<Long, Long> restaurantAssignments
    ) {
        if (restaurantRepository == null
                || userRepo == null
                || restaurantAssignments == null) {

            throw new IllegalArgumentException(
                    "MenuService dependencies cannot be null"
            );
        }

        this.restaurantRepository = restaurantRepository;
        this.userRepo = userRepo;
        this.restaurantAssignments =
                restaurantAssignments;
    }

    // Identify the currently authenticated user
    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new SecurityException(
                    "User is not authenticated"
            );
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

    // Check permission to modify this restaurant's menu
    private void requireRestaurantManager(Long restaurantId) {

        User user = getAuthenticatedUser();

        // Operators can manage any restaurant
        if (user.getRole() == Role.OPERATOR) {
            return;
        }

        if (user.getRole() != Role.RESTAURANT) {
            throw new SecurityException(
                    "Restaurant or operator account required"
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

    // Find a restaurant or throw an error
    private Restaurant requireRestaurant(Long restaurantId) {

        if (restaurantId == null) {
            throw new IllegalArgumentException(
                    "Restaurant ID is required"
            );
        }

        Restaurant restaurant =
                restaurantRepository.findById(restaurantId);

        if (restaurant == null) {
            throw new IllegalArgumentException(
                    "Restaurant not found"
            );
        }

        return restaurant;
    }

    // Find an item belonging to a restaurant
    private MenuItem requireMenuItem(
            Restaurant restaurant,
            Long itemId
    ) {

        if (itemId == null) {
            throw new IllegalArgumentException(
                    "Menu item ID is required"
            );
        }

        for (MenuItem item : restaurant.getMenuItems()) {

            if (item.getItemId().equals(itemId)) {
                return item;
            }
        }

        throw new IllegalArgumentException(
                "Menu item not found in this restaurant"
        );
    }

    // GET A RESTAURANT'S MENU
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<MenuItem> getMenu(Long restaurantId) {

        User user = getAuthenticatedUser();

        Restaurant restaurant =
                requireRestaurant(restaurantId);

        // Spec Appendix A: restaurant staff may not see other restaurants' menus
        if (user.getRole() == Role.RESTAURANT) {
            Long assignedRestaurantId =
                    restaurantAssignments.get(user.getId());

            if (assignedRestaurantId == null
                    || !assignedRestaurantId.equals(restaurantId)) {
                throw new SecurityException(
                        "User is not authorized for this restaurant"
                );
            }
        }

        return restaurant.getMenuItems();
    }

    // GET ONE MENU ITEM
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public MenuItem getMenuItem(
            Long restaurantId,
            Long itemId
    ) {

        getAuthenticatedUser();

        Restaurant restaurant =
                requireRestaurant(restaurantId);

        return requireMenuItem(restaurant, itemId);
    }

    // MARK AN ITEM SOLD OUT
    public synchronized MenuItem markItemSoldOut(
            Long restaurantId,
            Long itemId
    ) {
        restaurantRepository.lockForWrite();

        requireRestaurantManager(restaurantId);

        Restaurant restaurant =
                requireRestaurant(restaurantId);

        MenuItem item =
                requireMenuItem(restaurant, itemId);

        item.markSoldOut();
        restaurantRepository.save(restaurant);

        return item;
    }

    // MARK AN ITEM AVAILABLE
    public synchronized MenuItem markItemAvailable(
            Long restaurantId,
            Long itemId
    ) {
        restaurantRepository.lockForWrite();

        requireRestaurantManager(restaurantId);

        Restaurant restaurant =
                requireRestaurant(restaurantId);

        MenuItem item =
                requireMenuItem(restaurant, itemId);

        item.markAvailable();
        restaurantRepository.save(restaurant);

        return item;
    }

    // ADD A NEW ITEM TO THE RESTAURANT MENU
    public synchronized MenuItem addMenuItem(
            Long restaurantId,
            MenuItem item
    ) {
        restaurantRepository.lockForWrite();

        requireRestaurantManager(restaurantId);

        Restaurant restaurant =
                requireRestaurant(restaurantId);

        // Restaurant.java validates null and duplicate item IDs
        restaurant.addMenuItem(item);
        restaurantRepository.save(restaurant);

        return item;
    }
}

