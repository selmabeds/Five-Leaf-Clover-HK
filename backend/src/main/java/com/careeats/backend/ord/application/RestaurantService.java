
package com.careeats.backend.ord.application;

import com.careeats.backend.ord.domain.Restaurant;
import com.careeats.backend.ord.infrastructure.RestaurantRepository;

import com.careeats.backend.reg.Role;
import com.careeats.backend.reg.User;
import com.careeats.backend.reg.UserRepo;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepo userRepo;

    // Restaurant user ID -> Restaurant ID
    private final Map<Long, Long> restaurantAssignments;

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            UserRepo userRepo,
            Map<Long, Long> restaurantAssignments
    ) {
        if (restaurantRepository == null
                || userRepo == null
                || restaurantAssignments == null) {

            throw new IllegalArgumentException(
                    "RestaurantService dependencies cannot be null"
            );
        }

        this.restaurantRepository = restaurantRepository;
        this.userRepo = userRepo;
        this.restaurantAssignments =
                new HashMap<>(restaurantAssignments);
    }

    // Find the currently authenticated user
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

    // Check permission to manage a restaurant
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

    // GET ALL RESTAURANTS
    public List<Restaurant> getAllRestaurants() {

        getAuthenticatedUser();

        return restaurantRepository.findAll();
    }

    // GET ONE RESTAURANT
    public Restaurant getRestaurant(Long restaurantId) {

        getAuthenticatedUser();

        return requireRestaurant(restaurantId);
    }

    // PAUSE RESTAURANT
    public synchronized Restaurant pauseRestaurant(Long restaurantId) {

        requireRestaurantManager(restaurantId);

        Restaurant restaurant =
                requireRestaurant(restaurantId);

        // Restaurant.java records the status transition
        restaurant.pause();

        return restaurant;
    }

    // REOPEN RESTAURANT
    public synchronized Restaurant reopenRestaurant(Long restaurantId) {

        requireRestaurantManager(restaurantId);

        Restaurant restaurant =
                requireRestaurant(restaurantId);

        // Restaurant.java records the status transition
        restaurant.reopen();

        return restaurant;
    }
}

