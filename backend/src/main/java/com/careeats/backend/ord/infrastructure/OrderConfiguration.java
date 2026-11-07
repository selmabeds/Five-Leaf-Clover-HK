package com.careeats.backend.ord.infrastructure;

import com.careeats.backend.ord.application.*;
import com.careeats.backend.persistence.*;
import com.careeats.backend.reg.infrastructure.UserRepo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;

@Configuration
public class OrderConfiguration {
    @Bean public RestaurantRepository restaurantRepository(JdbcTemplate jdbc) {
        return new JdbcRestaurantRepository(jdbc);
    }
    @Bean public OrderRepository orderRepository(JdbcTemplate jdbc) {
        return new JdbcOrderRepository(jdbc);
    }
    @Bean public Map<Long, Long> restaurantAssignments(JdbcTemplate jdbc) {
        return new JdbcRestaurantAssignments(jdbc);
    }
    @Bean public RestaurantService restaurantService(RestaurantRepository restaurants, UserRepo users,
                                                     Map<Long, Long> restaurantAssignments) {
        return new RestaurantService(restaurants, users, restaurantAssignments);
    }
    @Bean public MenuService menuService(RestaurantRepository restaurants, UserRepo users,
                                         Map<Long, Long> restaurantAssignments) {
        return new MenuService(restaurants, users, restaurantAssignments);
    }
    @Bean public OrderService orderService(OrderRepository orders, RestaurantRepository restaurants,
                                            UserRepo users, Map<Long, Long> restaurantAssignments) {
        return new OrderService(orders, restaurants, users, restaurantAssignments);
    }
}
