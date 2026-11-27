package com.careeats.backend.rvw.infrastructure;

import com.careeats.backend.rvw.domain.RestaurantRating;
import com.careeats.backend.rvw.domain.Review;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class ReviewRepository {
    private final JdbcTemplate jdbc;

    public ReviewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record OrderFacts(long customerId, long restaurantId, Long courierId,
                             String status, boolean delivered) {}

    public OrderFacts orderFacts(long orderId) {
        return jdbc.query("""
            SELECT o.customer_id,o.restaurant_id,o.status,d.order_id AS delivered_id,d.courier_id
            FROM careeats.orders o
            LEFT JOIN careeats.delivered_orders d ON d.order_id=o.id
            WHERE o.id=?
            """, (row, index) -> new OrderFacts(row.getLong("customer_id"),
                row.getLong("restaurant_id"), row.getObject("courier_id", Long.class),
                row.getString("status"), row.getObject("delivered_id") != null), orderId)
            .stream().findFirst().orElse(null);
    }

    public boolean exists(long orderId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM careeats.order_reviews WHERE order_id=?",
            Long.class, orderId) > 0;
    }

    public Review save(long orderId, OrderFacts order, int restaurantRating, int courierRating) {
        Instant now = Instant.now();
        jdbc.update("""
            INSERT INTO careeats.order_reviews(order_id,restaurant_rating,courier_rating,created_at)
            VALUES (?,?,?,?)
            """, orderId, restaurantRating, courierRating, now.atOffset(ZoneOffset.UTC));
        return new Review(orderId, order.restaurantId(), order.courierId(),
            restaurantRating, courierRating, now);
    }

    public List<Review> forCustomer(long customerId) {
        return jdbc.query("""
            SELECT r.*,d.restaurant_id,d.courier_id
            FROM careeats.order_reviews r JOIN careeats.delivered_orders d ON d.order_id=r.order_id
            WHERE d.customer_id=? ORDER BY r.created_at DESC,r.order_id DESC
            """, (row, index) -> new Review(row.getLong("order_id"), row.getLong("restaurant_id"),
                row.getLong("courier_id"), row.getInt("restaurant_rating"), row.getInt("courier_rating"),
                row.getObject("created_at", OffsetDateTime.class).toInstant()), customerId);
    }

    public RestaurantRating restaurantRating(long restaurantId) {
        return jdbc.query("""
            SELECT COUNT(*) AS review_count,AVG(CAST(r.restaurant_rating AS DECIMAL(10,2))) AS average_rating
            FROM careeats.order_reviews r JOIN careeats.delivered_orders d ON d.order_id=r.order_id
            WHERE d.restaurant_id=?
            """, (row, index) -> {
                BigDecimal average = row.getBigDecimal("average_rating");
                return new RestaurantRating(restaurantId, row.getLong("review_count"),
                    average == null ? null : average.setScale(2, RoundingMode.HALF_UP));
            }, restaurantId).get(0);
    }

    public boolean restaurantExists(long restaurantId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM careeats.restaurants WHERE id=?",
            Long.class, restaurantId) > 0;
    }
}
