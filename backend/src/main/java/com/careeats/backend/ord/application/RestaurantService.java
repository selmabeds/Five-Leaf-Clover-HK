
package com.careeats.backend.ord.application;

import com.careeats.backend.ord.domain.Restaurant;
import com.careeats.backend.ord.infrastructure.RestaurantRepository;

import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import java.util.List;
import java.util.Map;

@org.springframework.transaction.annotation.Transactional
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

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
        this.authenticatedUserProvider = new AuthenticatedUserProvider(userRepo);
        this.restaurantAssignments =
                restaurantAssignments;
    }

    // Check permission to manage a restaurant
    private void requireRestaurantManager(Long restaurantId) {

        User user = authenticatedUserProvider.getAuthenticatedUser();

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
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<Restaurant> getAllRestaurants() {

        authenticatedUserProvider.getAuthenticatedUser();

        return restaurantRepository.findAll();
    }

    // GET ONE RESTAURANT
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Restaurant getRestaurant(Long restaurantId) {

        authenticatedUserProvider.getAuthenticatedUser();

        return requireRestaurant(restaurantId);
    }

    // PAUSE RESTAURANT
    public synchronized Restaurant pauseRestaurant(Long restaurantId) {
        restaurantRepository.lockForWrite();

        requireRestaurantManager(restaurantId);

        Restaurant restaurant =
                requireRestaurant(restaurantId);

        // Restaurant.java records the status transition
        restaurant.pause();
        restaurantRepository.save(restaurant);

        return restaurant;
    }

    // REOPEN RESTAURANT
    public synchronized Restaurant reopenRestaurant(Long restaurantId) {
        restaurantRepository.lockForWrite();

        requireRestaurantManager(restaurantId);

        Restaurant restaurant =
                requireRestaurant(restaurantId);

        // Restaurant.java records the status transition
        restaurant.reopen();
        restaurantRepository.save(restaurant);

        return restaurant;
    }
}

