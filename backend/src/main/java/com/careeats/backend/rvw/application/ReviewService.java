package com.careeats.backend.rvw.application;

import com.careeats.backend.ord.application.AuthenticatedUserProvider;
import com.careeats.backend.persistence.DatabaseLock;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;
import com.careeats.backend.rvw.domain.RestaurantRating;
import com.careeats.backend.rvw.domain.Review;
import com.careeats.backend.rvw.infrastructure.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ReviewService {
    private final ReviewRepository reviews;
    private final JdbcTemplate jdbc;
    private final AuthenticatedUserProvider users;

    public ReviewService(ReviewRepository reviews, JdbcTemplate jdbc, UserRepo users) {
        this.reviews = reviews;
        this.jdbc = jdbc;
        this.users = new AuthenticatedUserProvider(users);
    }

    public Review submit(long orderId, Integer restaurantRating, Integer courierRating) {
        User customer = requireCustomer();
        validateRating(restaurantRating, "Restaurant");
        validateRating(courierRating, "Courier");
        if (orderId <= 0) throw new IllegalArgumentException("A positive order ID is required");

        DatabaseLock.acquire(jdbc);
        var order = reviews.orderFacts(orderId);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        if (order.customerId() != customer.getId()) {
            throw new SecurityException("Only the customer who placed this order can rate it");
        }
        if (!order.delivered() || order.courierId() == null
            || !List.of("DELIVERED", "REFUNDED").contains(order.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a delivered order can be rated");
        }
        if (reviews.exists(orderId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order has already been rated");
        }
        return reviews.save(orderId, order, restaurantRating, courierRating);
    }

    @Transactional(readOnly = true)
    public List<Review> myReviews() {
        return reviews.forCustomer(requireCustomer().getId());
    }

    @Transactional(readOnly = true)
    public RestaurantRating restaurantRating(long restaurantId) {
        users.getAuthenticatedUser();
        if (!reviews.restaurantExists(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        return reviews.restaurantRating(restaurantId);
    }

    private User requireCustomer() {
        User user = users.getAuthenticatedUser();
        if (user.getRole() != Role.CUSTOMER) throw new SecurityException("Customer account required");
        return user;
    }

    private void validateRating(Integer rating, String target) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException(target + " rating must be a whole positive number from 1 to 5");
        }
    }
}
