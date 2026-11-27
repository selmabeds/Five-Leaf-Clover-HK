package com.careeats.backend.ord.application;

import com.careeats.backend.ord.domain.MenuItem;
import com.careeats.backend.ord.domain.Order;
import com.careeats.backend.ord.domain.OrderLine;
import com.careeats.backend.ord.domain.Restaurant;
import com.careeats.backend.ord.domain.RestaurantStatus;
import com.careeats.backend.ord.infrastructure.OrderRepository;
import com.careeats.backend.ord.infrastructure.RestaurantRepository;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class AuthenticationCharacterizationTest {

    private static final Long CUSTOMER_ID = 100L;
    private static final Long RESTAURANT_ID = 10L;
    private static final String EMAIL = "customer@example.com";

    private UserRepo userRepo;
    private OrderRepository orderRepository;
    private RestaurantRepository restaurantRepository;
    private OrderService orderService;
    private MenuService menuService;
    private RestaurantService restaurantService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        userRepo = new UserRepo();
        orderRepository = new OrderRepository();
        restaurantRepository = new RestaurantRepository();
        orderService = new OrderService(orderRepository, restaurantRepository, userRepo, Map.of());
        menuService = new MenuService(restaurantRepository, userRepo, Map.of());
        restaurantService = new RestaurantService(restaurantRepository, userRepo, Map.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingAuthenticationIsRejected() {
        assertAuthenticationRejected("User is not authenticated");
    }

    @Test
    void unauthenticatedTokenIsRejected() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, null)
        );

        assertAuthenticationRejected("User is not authenticated");
    }

    @Test
    void anonymousAuthenticationIsRejected() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "test-key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
                )
        );

        assertAuthenticationRejected("User is not authenticated");
    }

    @Test
    void authenticatedUserMissingFromRepositoryIsRejected() {
        authenticateCustomer();

        assertAuthenticationRejected("Authenticated user not found");
    }

    @Test
    void validAuthenticatedCustomerCanReadOrdersMenuAndRestaurant() {
        userRepo.save(new User(CUSTOMER_ID, "Test Customer", EMAIL, "hashedPassword", Role.CUSTOMER));
        authenticateCustomer();

        MenuItem item = new MenuItem(1L, "Soup", BigDecimal.valueOf(5), List.of("Carrots"));
        Restaurant restaurant = new Restaurant(RESTAURANT_ID, "Test Restaurant", 45, -73, RestaurantStatus.OPEN);
        restaurant.addMenuItem(item);
        restaurantRepository.save(restaurant);

        Order order = new Order(1L, CUSTOMER_ID, RESTAURANT_ID, List.of(new OrderLine(item.getItemId(), 1)));
        orderRepository.save(order);

        assertAll(
                () -> assertEquals(List.of(order), orderService.getCustomerOrders()),
                () -> assertEquals(List.of(item), menuService.getMenu(RESTAURANT_ID)),
                () -> assertSame(restaurant, restaurantService.getRestaurant(RESTAURANT_ID))
        );
    }

    private void authenticateCustomer() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, null, List.of())
        );
    }

    private void assertAuthenticationRejected(String message) {
        assertAll(
                () -> assertAuthenticationFailure(message, orderService::getCustomerOrders),
                () -> assertAuthenticationFailure(message, () -> menuService.getMenu(RESTAURANT_ID)),
                () -> assertAuthenticationFailure(message, () -> restaurantService.getRestaurant(RESTAURANT_ID))
        );
    }

    private void assertAuthenticationFailure(String message, Executable action) {
        SecurityException exception = assertThrowsExactly(SecurityException.class, action);
        assertEquals(message, exception.getMessage());
    }
}
