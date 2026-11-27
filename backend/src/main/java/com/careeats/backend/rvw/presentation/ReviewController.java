package com.careeats.backend.rvw.presentation;

import com.careeats.backend.rvw.application.ReviewService;
import com.careeats.backend.rvw.domain.RestaurantRating;
import com.careeats.backend.rvw.domain.Review;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    public record SubmitReview(BigDecimal restaurantRating, BigDecimal courierRating) {}

    @PostMapping("/orders/{orderId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Review submit(@PathVariable long orderId, @RequestBody SubmitReview request) {
        return reviews.submit(orderId, score(request.restaurantRating()), score(request.courierRating()));
    }

    private Integer score(BigDecimal value) {
        if (value == null) return null;
        try {
            return value.intValueExact();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("Ratings must be whole numbers from 1 to 5");
        }
    }

    @GetMapping("/mine")
    public List<Review> myReviews() {
        return reviews.myReviews();
    }

    @GetMapping("/restaurants/{restaurantId}")
    public RestaurantRating restaurantRating(@PathVariable long restaurantId) {
        return reviews.restaurantRating(restaurantId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException error) {
        return Map.of("error", error.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> forbidden(SecurityException error) {
        return Map.of("error", error.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> refused(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("error", error.getReason()));
    }
}
