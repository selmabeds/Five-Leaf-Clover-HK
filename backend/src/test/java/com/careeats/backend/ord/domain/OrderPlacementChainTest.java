package com.careeats.backend.ord.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderPlacementChainTest {

    // Same chain order as OrderService
    private OrderPlacementHandler buildChain() {
        OrderPlacementHandler first = new RestaurantExistsHandler();

        first.setNext(new RestaurantOpenHandler())
                .setNext(new OrderNotEmptyHandler())
                .setNext(new NoDuplicateItemsHandler())
                .setNext(new ItemBelongsToRestaurantHandler())
                .setNext(new ItemAvailableHandler());

        return first;
    }

    // Restaurant 1 with menu items 10 and 20, both available
    private Restaurant restaurant(RestaurantStatus status) {
        Restaurant restaurant = new Restaurant(1L, "Test Bistro", 45.5, -73.6, status);
        restaurant.addMenuItem(new MenuItem(10L, "Salad", new BigDecimal("9.99"), List.of("lettuce")));
        restaurant.addMenuItem(new MenuItem(20L, "Soup", new BigDecimal("6.50"), List.of("tomato")));
        return restaurant;
    }

    private List<OrderLine> lines(Long... itemIds) {
        List<OrderLine> lines = new ArrayList<>();
        for (Long id : itemIds) {
            lines.add(new OrderLine(id, 1));
        }
        return lines;
    }

    private void place(Restaurant restaurant, List<OrderLine> lines) {
        buildChain().handle(new OrderPlacementRequest(1L, restaurant, lines));
    }

    @Test
    void validOrderPassesWholeChain() {
        assertDoesNotThrow(() -> place(restaurant(RestaurantStatus.OPEN), lines(10L, 20L)));
    }

    @Test
    void missingRestaurantIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> place(null, lines(10L)));

        assertEquals("Restaurant not found", e.getMessage());
    }

    @Test
    void pausedRestaurantIsRejected() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> place(restaurant(RestaurantStatus.PAUSED), lines(10L)));

        assertEquals("Restaurant is not accepting orders", e.getMessage());
    }

    @Test
    void closedRestaurantIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> place(restaurant(RestaurantStatus.CLOSED), lines(10L)));
    }

    @Test
    void emptyOrderIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> place(restaurant(RestaurantStatus.OPEN), new ArrayList<>()));
    }

    @Test
    void nullOrderLineIsRejected() {
        List<OrderLine> withNull = lines(10L);
        withNull.add(null);

        assertThrows(IllegalArgumentException.class,
                () -> place(restaurant(RestaurantStatus.OPEN), withNull));
    }

    @Test
    void duplicateItemIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> place(restaurant(RestaurantStatus.OPEN), lines(10L, 10L)));
    }

    @Test
    void itemFromAnotherRestaurantIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> place(restaurant(RestaurantStatus.OPEN), lines(99L)));

        assertEquals("Menu item does not belong to this restaurant: 99", e.getMessage());
    }

    @Test
    void soldOutItemIsRejected() {
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        restaurant.getMenuItems().get(1).markSoldOut();   // item 20

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> place(restaurant, lines(10L, 20L)));

        assertEquals("Menu item is sold out: 20", e.getMessage());
    }

    @Test
    void chainOrderDecidesWhichRuleFailsFirst() {
        // Paused restaurant AND sold-out item: the open check runs first
        Restaurant restaurant = restaurant(RestaurantStatus.PAUSED);
        restaurant.getMenuItems().get(0).markSoldOut();

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> place(restaurant, lines(10L)));

        assertEquals("Restaurant is not accepting orders", e.getMessage());
    }
}