
package com.careeats.backend.ord.application;

import com.careeats.backend.ord.domain.Order;
import com.careeats.backend.ord.domain.OrderLine;
import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.ord.infrastructure.OrderRepository;
import com.careeats.backend.ord.infrastructure.RestaurantRepository;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderCancellationCharacterizationTest {

    private OrderRepository orderRepository;
    private RestaurantRepository restaurantRepository;
    private UserRepo userRepo;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        orderRepository = spy(new OrderRepository());
        restaurantRepository = mock(RestaurantRepository.class);
        userRepo = new UserRepo();

        orderService = new OrderService(
                orderRepository,
                restaurantRepository,
                userRepo,
                Map.of()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, Role role) {
        String email = "test@careeats.com";

        User user = new User(
                userId,
                "Test User",
                email,
                "hashedPassword",
                role
        );

        userRepo.save(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of()
                )
        );
    }

    private Order createOrder() {
        Order order = new Order(
                1L,
                100L,
                10L,
                List.of(new OrderLine(1L, 1))
        );

        orderRepository.save(order);
        clearInvocations(orderRepository);

        return order;
    }

    // TC1
    @Test
    void customerCanCancelOwnPlacedOrder() {
        authenticate(100L, Role.CUSTOMER);
        Order order = createOrder();

        Order result = orderService.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals(2, order.getTransitionHistory().size());
        assertEquals(OrderStatus.PLACED,
                order.getTransitionHistory().get(1).getPreviousStatus());
        assertEquals(OrderStatus.CANCELLED,
                order.getTransitionHistory().get(1).getNewStatus());

        verify(orderRepository).save(order);
    }

    // TC2
    @Test
    void customerCannotCancelAnotherCustomersOrder() {
        authenticate(200L, Role.CUSTOMER);
        Order order = createOrder();

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> orderService.cancelOrder(1L)
        );

        assertEquals(
                "Customers can only cancel their own orders",
                exception.getMessage()
        );

        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(1, order.getTransitionHistory().size());

        verify(orderRepository, never()).save(any(Order.class));
    }

    // TC3
    @Test
    void customerCannotCancelAcceptedOrder() {
        authenticate(100L, Role.CUSTOMER);
        Order order = createOrder();
        order.accept();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.cancelOrder(1L)
        );

        assertEquals(
                "Cannot cancel order in the following status: ACCEPTED",
                exception.getMessage()
        );

        assertEquals(OrderStatus.ACCEPTED, order.getStatus());
        assertEquals(2, order.getTransitionHistory().size());

        verify(orderRepository, never()).save(any(Order.class));
    }

    // TC4
    @Test
    void operatorCanCancelPlacedOrder() {
        authenticate(200L, Role.OPERATOR);
        Order order = createOrder();

        Order result = orderService.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals(2, order.getTransitionHistory().size());

        verify(orderRepository).save(order);
    }

    // TC5
    @Test
    void operatorCanCancelAcceptedOrder() {
        authenticate(200L, Role.OPERATOR);
        Order order = createOrder();
        order.accept();

        when(restaurantRepository.existsById(10L))
                .thenReturn(true);

        assertEquals(
                1,
                orderRepository.findAcceptedForRestaurant(10L).size()
        );

        Order result = orderService.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals(3, order.getTransitionHistory().size());

        assertTrue(
                orderRepository.findAcceptedForRestaurant(10L).isEmpty()
        );

        verify(orderRepository).save(order);
    }

    // TC6
    @Test
    void operatorCannotCancelAcceptedOrderMissingFromQueue() {
        authenticate(200L, Role.OPERATOR);
        Order order = createOrder();
        order.accept();

        when(restaurantRepository.existsById(10L))
                .thenReturn(true);

        doReturn(List.of())
                .when(orderRepository)
                .findAcceptedForRestaurant(10L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.cancelOrder(1L)
        );

        assertEquals(
                "Accepted order is missing from preparation queue",
                exception.getMessage()
        );

        assertEquals(OrderStatus.ACCEPTED, order.getStatus());
        assertEquals(2, order.getTransitionHistory().size());

        verify(orderRepository, never()).save(any(Order.class));
    }

    // TC7
    @Test
    void restaurantStaffCannotCancelOrder() {
        authenticate(200L, Role.RESTAURANT);
        Order order = createOrder();

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> orderService.cancelOrder(1L)
        );

        assertEquals(
                "Only customers and operators can cancel orders",
                exception.getMessage()
        );

        assertEquals(OrderStatus.PLACED, order.getStatus());

        verify(orderRepository, never()).save(any(Order.class));
    }

    // TC8
    @Test
    void unauthenticatedUserCannotCancelOrder() {
        Order order = createOrder();

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> orderService.cancelOrder(1L)
        );

        assertEquals(
                "User is not authenticated",
                exception.getMessage()
        );

        assertEquals(OrderStatus.PLACED, order.getStatus());

        verify(orderRepository, never()).save(any(Order.class));
    }

    // TC9
    @Test
    void operatorCannotCancelAlreadyCancelledOrder() {
        authenticate(200L, Role.OPERATOR);
        Order order = createOrder();
        order.cancel();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.cancelOrder(1L)
        );

        assertEquals(
                "Cannot cancel order by operator in the following status: CANCELLED",
                exception.getMessage()
        );

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(2, order.getTransitionHistory().size());

        verify(orderRepository, never()).save(any(Order.class));
    }
}
