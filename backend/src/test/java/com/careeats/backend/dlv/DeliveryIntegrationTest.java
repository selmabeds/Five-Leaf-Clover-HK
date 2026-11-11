package com.careeats.backend.dlv;

import com.careeats.backend.dlv.application.DeliveryService;
import com.careeats.backend.dlv.domain.Assignment;
import com.careeats.backend.dlv.domain.CourierState;
import com.careeats.backend.dlv.infrastructure.DeliveryRepository;
import com.careeats.backend.ord.application.OrderService;
import com.careeats.backend.ord.domain.OrderLine;
import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.ord.infrastructure.OrderRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dlv-integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=true",
        "careeats.seed.enabled=false"
})
class DeliveryIntegrationTest {
    @Autowired DeliveryService delivery;
    @Autowired DeliveryRepository deliveries;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext web;

    @BeforeEach
    void prepareDatabase() {
        // This database belongs exclusively to this test class.
        List<String> tables = List.of(
                "priced_lines", "fare_snapshots", "ledger_entries",
                "delivered_orders", "delivery_assignments", "couriers",
                "order_transitions", "order_lines", "orders",
                "menu_transitions", "menu_ingredients", "menu_items",
                "restaurant_transitions", "restaurant_assignments",
                "restaurants", "app_users"
        );

        for (String table : tables) {
            jdbc.update("DELETE FROM careeats." + table);
        }

        addUser(1, "customer@test.local", "CUSTOMER");
        addUser(2, "staff@test.local", "RESTAURANT");
        addUser(3, "courier1@test.local", "COURIER");
        addUser(4, "operator@test.local", "OPERATOR");
        addUser(5, "courier2@test.local", "COURIER");

        jdbc.update("""
                INSERT INTO careeats.restaurants
                    (id, name, latitude, longitude, status)
                VALUES (1, 'Test Restaurant', 45.5, -73.6, 'OPEN')
                """);

        jdbc.update("""
                INSERT INTO careeats.restaurant_assignments
                    (user_id, restaurant_id)
                VALUES (2, 1)
                """);

        jdbc.update("""
                INSERT INTO careeats.menu_items
                    (restaurant_id, item_id, position, name, price, status)
                VALUES (1, 1, 0, 'Test Meal', 19.99, 'AVAILABLE')
                """);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void offersOrderToNearestAvailableCourier() {
        goOnDuty("courier1@test.local");
        goOnDuty("courier2@test.local");

        setPosition(3, 45.6, -73.6);
        setPosition(5, 45.5, -73.6);

        long orderId = readyOrder();

        assertNull(deliveries.findAssignmentForCourier(3));
        assertEquals(orderId,
                deliveries.findAssignmentForCourier(5).orderId());
    }

    @Test
    void breaksDistanceTieByLowestCourierId() {
        goOnDuty("courier2@test.local");
        goOnDuty("courier1@test.local");

        setPosition(3, 45.5, -73.6);
        setPosition(5, 45.5, -73.6);

        long orderId = readyOrder();

        assertEquals(orderId,
                deliveries.findAssignmentForCourier(3).orderId());
        assertNull(deliveries.findAssignmentForCourier(5));
    }

    @Test
    void retriesQueueAfterDeliveryAndDoesNotChargeTwice() {
        long first = readyOrder();
        long second = readyOrder();

        login("operator@test.local");
        assertEquals(List.of(first, second),
                delivery.dispatchView().queuedOrderIds());

        login("courier1@test.local");
        assertEquals(first, delivery.goOnDuty().assignment().orderId());

        assertThrows(IllegalStateException.class,
                () -> delivery.confirmPickup(first));

        delivery.acceptOffer(first);
        delivery.confirmPickup(first);

        assertThrows(IllegalStateException.class, delivery::goOffDuty);

        var result = delivery.confirmDelivery(first);
        assertEquals(OrderStatus.DELIVERED,
                orders.findById(first).getStatus());
        assertEquals(second, result.assignment().orderId());
        assertEquals(Assignment.Status.OFFERED,
                result.assignment().status());
        assertEquals(1L, chargeCount(first));

        var confirmation = deliveries.findConfirmedDelivery(first);

        // A retry must preserve both billing facts and the next assignment.
        var retry = delivery.confirmDelivery(first);
        assertEquals(second, retry.assignment().orderId());
        assertEquals(confirmation, deliveries.findConfirmedDelivery(first));
        assertEquals(1L, chargeCount(first));

        login("courier2@test.local");
        assertThrows(SecurityException.class,
                () -> delivery.confirmDelivery(first));
    }

    @Test
    void acceptedOrderStaysReservedUntilOperatorReassignsIt() {
        long orderId = readyOrder();

        login("courier1@test.local");
        delivery.goOnDuty();
        delivery.acceptOffer(orderId);

        var offline = delivery.goOffDuty();
        assertEquals(CourierState.OFFLINE, offline.courier().state());
        assertEquals(Assignment.Status.STALLED,
                offline.assignment().status());

        assertThrows(IllegalStateException.class, delivery::goOnDuty);

        login("courier2@test.local");
        assertNull(delivery.goOnDuty().assignment());
        assertThrows(SecurityException.class,
                () -> delivery.reassignStalled(orderId));

        login("operator@test.local");
        delivery.reassignStalled(orderId);

        login("courier2@test.local");
        assertEquals(orderId,
                delivery.currentCourier().assignment().orderId());

        // Declining the new offer releases it and takes the courier offline.
        assertEquals(CourierState.OFFLINE,
                delivery.declineOffer(orderId).courier().state());

        login("operator@test.local");
        assertEquals(List.of(orderId),
                delivery.dispatchView().queuedOrderIds());
    }

    @Test
    void rollsBackDeliveryWhenChargeCannotBeSaved() {
        long orderId = readyOrder();

        login("courier1@test.local");
        delivery.goOnDuty();
        delivery.acceptOffer(orderId);
        delivery.confirmPickup(orderId);

        // Force a database failure when PRC tries to insert the charge.
        jdbc.execute("""
                ALTER TABLE careeats.ledger_entries
                ADD CONSTRAINT dlv_test_reject_charge CHECK (amount < 0)
                """);

        try {
            assertThrows(RuntimeException.class,
                    () -> delivery.confirmDelivery(orderId));

            assertEquals(OrderStatus.PICKED_UP,
                    orders.findById(orderId).getStatus());
            assertEquals(CourierState.CARRYING,
                    delivery.currentCourier().courier().state());
            assertEquals(Assignment.Status.ACCEPTED,
                    deliveries.findAssignmentForOrder(orderId).status());
            assertNull(deliveries.findConfirmedDelivery(orderId));
            assertEquals(0L, chargeCount(orderId));
        } finally {
            jdbc.execute("""
                    ALTER TABLE careeats.ledger_entries
                    DROP CONSTRAINT dlv_test_reject_charge
                    """);
        }

        delivery.confirmDelivery(orderId);
        assertEquals(OrderStatus.DELIVERED,
                orders.findById(orderId).getStatus());
        assertEquals(1L, chargeCount(orderId));
    }

    @Test
    void endpointsRequireAuthenticationRoleAndCsrf() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(web)
                .apply(springSecurity())
                .build();

        mvc.perform(get("/api/delivery/me"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/delivery/me")
                        .with(user("customer@test.local").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/delivery/dispatch")
                        .with(user("courier1@test.local").roles("COURIER")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/delivery/on-duty")
                        .with(user("courier1@test.local").roles("COURIER")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/delivery/on-duty")
                        .with(user("courier1@test.local").roles("COURIER"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    private long readyOrder() {
        login("customer@test.local");
        long orderId = orderService.placeOrder(
                1L, List.of(new OrderLine(1L, 1))).getOrderId();

        login("staff@test.local");
        orderService.acceptOrder(orderId);
        orderService.startNextOrder(1L);
        orderService.markOrderReady(orderId);

        return orderId;
    }

    private void goOnDuty(String email) {
        login(email);
        delivery.goOnDuty();
    }

    private void setPosition(long courierId, double latitude, double longitude) {
        jdbc.update("""
                UPDATE careeats.couriers
                SET latitude = ?, longitude = ?
                WHERE id = ?
                """,
                latitude, longitude, courierId);
    }

    private long chargeCount(long orderId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM careeats.ledger_entries
                WHERE order_id = ? AND entry_type = 'CHARGE'
                """,
                Long.class, orderId);
    }

    private void addUser(long id, String email, String role) {
        jdbc.update("""
                INSERT INTO careeats.app_users
                    (id, name, email, password_hash, role)
                VALUES (?, ?, ?, 'unused-in-this-test', ?)
                """,
                id, email, email, role);
    }

    private void login(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, "unused", List.of()));
    }
}