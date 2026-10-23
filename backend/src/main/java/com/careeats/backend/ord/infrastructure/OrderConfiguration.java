package com.careeats.backend.ord.infrastructure;

import com.careeats.backend.ord.application.MenuService;
import com.careeats.backend.ord.application.OrderService;
import com.careeats.backend.ord.application.RestaurantService;
import com.careeats.backend.ord.domain.MenuItem;
import com.careeats.backend.ord.domain.Restaurant;
import com.careeats.backend.ord.domain.RestaurantStatus;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OrderConfiguration {

    @Bean
    public RestaurantRepository restaurantRepository() {
        RestaurantRepository repository = new RestaurantRepository();

        Restaurant restaurant = new Restaurant(
            1L,
            "CARE Eats Test Restaurant",
            45.5017,
            -73.5673,
            RestaurantStatus.OPEN
        );

        restaurant.addMenuItem(new MenuItem(
            1L,
            "Vegetable Bowl",
            new BigDecimal("12.99"),
            List.of("Rice", "Carrots", "Broccoli")
        ));

        restaurant.addMenuItem(new MenuItem(
            2L,
            "Chicken Wrap",
            new BigDecimal("10.99"),
            List.of("Chicken", "Lettuce", "Tortilla")
        ));

        repository.save(restaurant);
        return repository;
    }

    @Bean
    public OrderRepository orderRepository() {
        return new OrderRepository();
    }

   @Bean
    public Map<Long, Long> restaurantAssignments() {
        Map<Long, Long> assignments = new HashMap<>();

        assignments.put(2L, 1L);

        return assignments;
    }

    @Bean
    public RestaurantService restaurantService(
            RestaurantRepository restaurantRepository,
            UserRepo userRepo,
            Map<Long, Long> restaurantAssignments) {

        return new RestaurantService(
            restaurantRepository, userRepo, restaurantAssignments
        );
    }

    @Bean
    public MenuService menuService(
            RestaurantRepository restaurantRepository,
            UserRepo userRepo,
            Map<Long, Long> restaurantAssignments) {

        return new MenuService(
            restaurantRepository, userRepo, restaurantAssignments
        );
    }

    @Bean
    public OrderService orderService(
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            UserRepo userRepo,
            Map<Long, Long> restaurantAssignments) {

        return new OrderService(
            orderRepository,
            restaurantRepository,
            userRepo,
            restaurantAssignments
        );
    }
}
