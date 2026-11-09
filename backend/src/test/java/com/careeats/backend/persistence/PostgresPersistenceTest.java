package com.careeats.backend.persistence;

import com.careeats.backend.ord.application.*;
import com.careeats.backend.ord.domain.*;
import com.careeats.backend.ord.domain.Order;
import com.careeats.backend.ord.infrastructure.*;
import com.careeats.backend.prc.application.PricingService;
import com.careeats.backend.prc.domain.*;
import com.careeats.backend.prc.infrastructure.LedgerRepository;
import com.careeats.backend.reg.application.RegistrationService;
import com.careeats.backend.reg.domain.*;
import com.careeats.backend.reg.infrastructure.UserRepo;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real, disposable local PostgreSQL. No Supabase credentials or Docker required. */
@ActiveProfiles("test")
@SpringBootTest(properties = {"careeats.seed.enabled=true", "spring.flyway.enabled=true",
    "spring.datasource.hikari.maximum-pool-size=3"})
class PostgresPersistenceTest {
    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        String external = System.getProperty("careeats.test.postgres.url");
        if (external != null) {
            // Optional disposable local server when Windows restrictions prevent pg_ctl launching.
            // Do not permit this escape hatch to point at Supabase or another remote database.
            if (!external.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/careeats_test")) {
                throw new IllegalArgumentException("Test override must target a disposable loopback careeats_test database");
            }
            String databaseName = "careeats_test_" + UUID.randomUUID().toString().replace("-", "");
            String server = external.substring(0, external.lastIndexOf('/') + 1);
            try (var connection = java.sql.DriverManager.getConnection(server + "postgres", "postgres", "");
                 var statement = connection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE " + databaseName);
            }
            properties.add("spring.datasource.url", () -> server + databaseName);
        } else {
            postgres = EmbeddedPostgres.builder().setPort(0).start();
            properties.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        }
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @AfterAll static void stop() throws Exception { if (postgres != null) postgres.close(); }
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepo users;
    @Autowired RegistrationService registration;
    @Autowired RestaurantRepository restaurants;
    @Autowired OrderRepository orders;
    @Autowired LedgerRepository ledger;
    @Autowired OrderService orderService;
    @Autowired MenuService menus;
    @Autowired RestaurantService restaurantService;
    @Autowired PricingService pricing;
    @Autowired DemoDataLoader seeder;
    @Autowired PlatformTransactionManager transactions;
    @Autowired WebApplicationContext web;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired com.careeats.backend.prc.application.OrderHistoryService prcHistory;

    private void login(String email) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(email, "unused", List.of()));
    }

    private Order place() {
        login("customer@careeats.com");
        return orderService.placeOrder(1L, List.of(new OrderLine(1L, 1)));
    }

    private TransactionTemplate tx() { return new TransactionTemplate(transactions); }

    private MockHttpSession sessionFor(String email, String password) throws Exception {
        SecurityContextHolder.clearContext();
        var mvc = MockMvcBuilders.webAppContextSetup(web).apply(springSecurity()).build();
        var result = mvc.perform(post("/login").with(csrf()).param("username", email).param("password", password))
            .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test void prcHttpHistoryIsScopedAndProtectsUnbilledReceipts() throws Exception {
        Order order = place();
        SecurityContextHolder.clearContext();
        var mvc = MockMvcBuilders.webAppContextSetup(web).apply(springSecurity()).build();
        mvc.perform(get("/api/prc/history")).andExpect(status().isUnauthorized());
        var customer = sessionFor("customer@careeats.com", "Customer123!");
        mvc.perform(get("/api/prc/context").session(customer)).andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("CUSTOMER")).andExpect(jsonPath("$.userId").value(1));
        String content = mvc.perform(get("/api/prc/history").session(customer)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        var rows = new tools.jackson.databind.ObjectMapper().readTree(content);
        assertFalse(rows.isEmpty());
        for (var row : rows) assertEquals(1L, row.get("customerId").asLong());
        mvc.perform(get("/api/prc/orders/" + order.getOrderId()).session(customer))
            .andExpect(status().isOk()).andExpect(jsonPath("$.lines[0].description").value("Vegetable Bowl"));
        mvc.perform(get("/api/prc/orders/" + order.getOrderId() + "/receipt").session(customer))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/prc/orders/100").session(customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/prc/orders/9999999").session(customer)).andExpect(status().isNotFound());
        mvc.perform(post("/api/prc/orders/" + order.getOrderId() + "/refund").session(customer).with(csrf()))
            .andExpect(status().isForbidden());
        var staff = sessionFor("staff5@example.test", "DemoOnly123!");
        mvc.perform(get("/api/prc/context").session(staff)).andExpect(status().isOk())
            .andExpect(jsonPath("$.restaurantId").value(5));
        var restaurantRows = new tools.jackson.databind.ObjectMapper().readTree(mvc.perform(
            get("/api/prc/history").session(staff)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(6, restaurantRows.size());
        for (var row : restaurantRows) assertEquals(5L, row.get("restaurantId").asLong());
        mvc.perform(get("/api/prc/orders/" + order.getOrderId()).session(staff)).andExpect(status().isForbidden());
        var operator = sessionFor("operator@careeats.com", "Operator123!");
        mvc.perform(get("/api/prc/history").session(operator)).andExpect(status().isOk());
        mvc.perform(post("/api/prc/orders/" + order.getOrderId() + "/refund").session(operator).with(csrf()))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/prc/orders/" + order.getOrderId() + "/refund").session(operator))
            .andExpect(status().isForbidden());
    }

    @Test void unchargedTerminalOrdersAppearWithoutFabricatedCharges() {
        var rejected = prcHistory.detailsFor(users.findByEmail("customer5@example.test"), 104L);
        var cancelled = prcHistory.detailsFor(users.findByEmail("customer6@example.test"), 105L);
        assertEquals(OrderStatus.REJECTED, rejected.status());
        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertNull(rejected.fare());
        assertTrue(rejected.entries().isEmpty());
        assertTrue(cancelled.entries().isEmpty());
        var rows = prcHistory.historyFor(users.findByEmail("customer5@example.test"));
        var row = rows.stream().filter(item -> item.orderId().equals(104L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.00"), ((OrderHistoryItem.Financial) row).totalCharge());
    }

    @Test void savedReceiptsSurviveRepositoryRecreationAndRefundsWithoutLeakingCourierPayments() throws Exception {
        Order order = place();
        var originalName = jdbc.queryForObject("SELECT item_name FROM careeats.order_lines WHERE order_id=?", String.class, order.getOrderId());
        var originalPrice = jdbc.queryForObject("SELECT unit_price FROM careeats.order_lines WHERE order_id=?", BigDecimal.class, order.getOrderId());
        tx().executeWithoutResult(status -> {
            DatabaseLock.acquire(jdbc);
            jdbc.update("UPDATE careeats.menu_items SET name='Renamed meal',price=99.99 WHERE restaurant_id=1 AND item_id=1");
        });
        try {
            Instant placed = Instant.parse("2026-11-01T12:00:00Z");
            pricing.recordDeliveredOrder(new DeliveredOrder(order.getOrderId(), 1L, 1L, 3L,
                List.of(new PricedOrderLine(1L, 1, originalPrice)), placed, placed.plusSeconds(1800)));
            var fresh = new com.careeats.backend.prc.application.OrderHistoryService(
                new com.careeats.backend.prc.infrastructure.JdbcOrderHistoryRepository(jdbc),
                new JdbcLedgerRepository(jdbc), new JdbcRestaurantAssignments(jdbc));
            var receipt = fresh.receiptFor(users.findByEmail("customer@careeats.com"), order.getOrderId());
            assertEquals(originalName, receipt.lines().get(0).description());
            assertEquals(originalPrice, receipt.lines().get(0).unitPrice());
            assertEquals(new BigDecimal("21.53"), receipt.fare().totalCharge());
            var mvc = MockMvcBuilders.webAppContextSetup(web).apply(springSecurity()).build();
            var courier = sessionFor("courier@careeats.com", "Courier123!");
            String courierJson = mvc.perform(get("/api/prc/history").session(courier)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            assertFalse(courierJson.contains("totalCharge"));
            assertFalse(courierJson.contains("refundedAmount"));
            assertFalse(courierJson.contains("customerId"));
            mvc.perform(get("/api/prc/orders/" + order.getOrderId() + "/receipt").session(courier))
                .andExpect(status().isForbidden());
            var operator = sessionFor("operator@careeats.com", "Operator123!");
            mvc.perform(post("/api/prc/orders/" + order.getOrderId() + "/refund").session(operator).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("REFUND"));
            mvc.perform(post("/api/prc/orders/" + order.getOrderId() + "/refund").session(operator).with(csrf()))
                .andExpect(status().isBadRequest());
            var refunded = fresh.receiptFor(users.findByEmail("operator@careeats.com"), order.getOrderId());
            assertEquals(OrderStatus.REFUNDED, refunded.status());
            assertEquals(2, refunded.entries().size());
            assertEquals(LedgerEntryType.CHARGE, refunded.entries().get(0).type());
            assertEquals(LedgerEntryType.REFUND, refunded.entries().get(1).type());
            assertEquals(new BigDecimal("0.00"), refunded.netCharge());
        } finally {
            tx().executeWithoutResult(status -> {
                DatabaseLock.acquire(jdbc);
                jdbc.update("UPDATE careeats.menu_items SET name=?,price=? WHERE restaurant_id=1 AND item_id=1", originalName, originalPrice);
            });
        }
    }

    @Test void listsUseBoundedQueriesAndFilterOrdersInDatabase() {
        CountingDataSource counting = new CountingDataSource(dataSource);
        JdbcOrderRepository batched = new JdbcOrderRepository(new JdbcTemplate(counting));
        assertTrue(batched.findAll().size() >= 84);
        assertEquals(3, counting.statements.get(), "Order count must not increase the query count");

        counting.statements.set(0);
        var restaurantOrders = batched.findByRestaurantId(5L);
        assertEquals(6, restaurantOrders.size());
        assertTrue(restaurantOrders.stream().allMatch(order -> order.getRestaurantId().equals(5L)));
        assertEquals(3, counting.statements.get());

        counting.statements.set(0);
        var customerOrders = batched.findByCustomerId(100L);
        assertFalse(customerOrders.isEmpty());
        assertTrue(customerOrders.stream().allMatch(order -> order.getCustomerId().equals(100L)));
        assertEquals(3, counting.statements.get());

        counting.statements.set(0);
        var catalog = new JdbcRestaurantRepository(new JdbcTemplate(counting)).findAll();
        assertEquals(14, catalog.size());
        assertEquals(37, catalog.stream().mapToInt(restaurant -> restaurant.getMenuItems().size()).sum());
        assertEquals(5, counting.statements.get(), "Menu and history queries must be batched across restaurants");
        var cake = catalog.stream().filter(restaurant -> restaurant.getRestaurantId().equals(2L))
            .findFirst().orElseThrow().getMenuItems().stream().filter(item -> item.getItemId().equals(5L))
            .findFirst().orElseThrow();
        assertEquals(MenuItemStatus.SOLD_OUT, cake.getStatus());
        assertEquals(List.of("Chocolate", "Eggs", "Butter"), cake.getIngredients());
        assertFalse(cake.getTransitionHistory().isEmpty());
    }

    @Test void slowCustomerReadDoesNotBlockRestaurantReadWhileHoldingPoolConnections() throws Exception {
        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        OrderRepository slow = new OrderRepository() {
            @Override public List<Order> findByCustomerId(Long id) {
                reading.countDown();
                try {
                    if (!release.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Test read was not released");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return orders.findByCustomerId(id);
            }
            @Override public List<Order> findByRestaurantId(Long id) { return orders.findByRestaurantId(id); }
        };
        OrderService sameService = new OrderService(slow, restaurants, users, new JdbcRestaurantAssignments(jdbc));
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<List<Order>> customer = workers.submit(() -> {
                login("customer1@example.test");
                try { return tx().execute(status -> sameService.getCustomerOrders()); }
                finally { SecurityContextHolder.clearContext(); }
            });
            assertTrue(reading.await(5, TimeUnit.SECONDS));
            Future<List<Order>> manager = workers.submit(() -> {
                login("restaurant@careeats.com");
                try { return tx().execute(status -> sameService.getRestaurantOrders(1L)); }
                finally { SecurityContextHolder.clearContext(); }
            });
            assertFalse(manager.get(5, TimeUnit.SECONDS).isEmpty(),
                "Independent reads must finish even while another request is waiting");
            release.countDown();
            assertFalse(customer.get(5, TimeUnit.SECONDS).isEmpty());
        } finally {
            release.countDown();
            workers.shutdownNow();
        }
    }

    private static final class CountingDataSource extends org.springframework.jdbc.datasource.AbstractDataSource {
        private final javax.sql.DataSource delegate;
        private final java.util.concurrent.atomic.AtomicInteger statements = new java.util.concurrent.atomic.AtomicInteger();
        private CountingDataSource(javax.sql.DataSource delegate) { this.delegate = delegate; }
        @Override public java.sql.Connection getConnection() throws java.sql.SQLException {
            return count(delegate.getConnection());
        }
        @Override public java.sql.Connection getConnection(String username, String password) throws java.sql.SQLException {
            return count(delegate.getConnection(username, password));
        }
        private java.sql.Connection count(java.sql.Connection connection) {
            return (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(
                java.sql.Connection.class.getClassLoader(), new Class<?>[]{java.sql.Connection.class}, (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement") || method.getName().equals("createStatement")) {
                        statements.incrementAndGet();
                    }
                    try { return method.invoke(connection, args); }
                    catch (java.lang.reflect.InvocationTargetException error) { throw error.getCause(); }
                });
        }
    }

    @Test void existingSessionLoginAndOrderJsonRemainCompatible() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(web).apply(springSecurity()).build();
        var login = mvc.perform(post("/login").with(csrf())
                .param("username", "customer@careeats.com").param("password", "Customer123!"))
            .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/restaurants").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].restaurantId").value(1));
        mvc.perform(post("/api/orders").session(session).with(csrf())
                .contentType("application/json")
                .content("{\"restaurantId\":1,\"orderLines\":[{\"menuItemId\":1,\"quantity\":1}]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PLACED"))
            .andExpect(jsonPath("$.orderLines[0].menuItemId").value(1));
    }

    @Test void seedsAreRepeatableAndPreserveChanges() throws Exception {
        assertEquals(14, restaurants.findAll().size());
        assertEquals(37, jdbc.queryForObject("SELECT COUNT(*) FROM careeats.menu_items", Integer.class));
        assertEquals(84, jdbc.queryForObject("SELECT COUNT(*) FROM careeats.orders WHERE id < 1000", Integer.class));
        assertEquals(46, jdbc.queryForObject("SELECT COUNT(*) FROM careeats.app_users WHERE id < 1000", Integer.class));
        login("restaurant@careeats.com");
        menus.markItemSoldOut(1L, 2L);
        seeder.run();
        assertEquals(MenuItemStatus.SOLD_OUT, new JdbcRestaurantRepository(jdbc).findById(1L).getMenuItems().get(1).getStatus());
        menus.markItemAvailable(1L, 2L);
    }

    @Test void restoresOriginalEventsAndPersistsAllLifecycleMutations() {
        Order placed = place();
        String eventId = placed.getTransitionHistory().get(0).getEventId();
        LocalDateTime timestamp = placed.getTransitionHistory().get(0).getTimestamp();
        login("restaurant@careeats.com");
        orderService.acceptOrder(placed.getOrderId());
        Order loaded = new JdbcOrderRepository(jdbc).findById(placed.getOrderId());
        assertEquals(OrderStatus.ACCEPTED, loaded.getStatus());
        assertEquals(2, loaded.getTransitionHistory().size());
        assertEquals(eventId, loaded.getTransitionHistory().get(0).getEventId());
        // PostgreSQL timestamps retain microseconds rather than Java's nanoseconds.
        assertTrue(Duration.between(timestamp, loaded.getTransitionHistory().get(0).getTimestamp()).abs()
            .compareTo(Duration.ofNanos(1000)) <= 0);
        orderService.rejectOrder(placeForRejection(), "Kitchen closed");
        assertEquals(2, new JdbcOrderRepository(jdbc).findById(placed.getOrderId()).getTransitionHistory().size());
    }

    private long placeForRejection() {
        long id = place().getOrderId();
        login("restaurant@careeats.com");
        return id;
    }

    @Test void fifoIsRecoveredByFreshServiceAndClaimedOnceAcrossInstances() throws Exception {
        Order first = place();
        Order second = place();
        login("restaurant@careeats.com");
        // Accept in reverse placement order; FIFO must track acceptance, not order ID.
        orderService.acceptOrder(second.getOrderId());
        orderService.acceptOrder(first.getOrderId());
        List<Long> expected = orders.findAcceptedForRestaurant(1L).stream().map(Order::getOrderId).toList();
        assertTrue(expected.indexOf(second.getOrderId()) < expected.indexOf(first.getOrderId()));
        Callable<Long> claim = () -> {
            login("restaurant@careeats.com");
            try {
                OrderService fresh = new OrderService(orders, restaurants, users, new JdbcRestaurantAssignments(jdbc));
                return tx().execute(status -> fresh.startNextOrder(1L).getOrderId());
            } finally { SecurityContextHolder.clearContext(); }
        };
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            List<Future<Long>> results = workers.invokeAll(List.of(claim, claim));
            Set<Long> actual = Set.of(results.get(0).get(20, TimeUnit.SECONDS), results.get(1).get(20, TimeUnit.SECONDS));
            assertEquals(new HashSet<>(expected.subList(0, 2)), actual);
        } finally { workers.shutdownNow(); }
        assertEquals(OrderStatus.PREPARING, orders.findById(expected.get(0)).getStatus());
    }

    @Test void idsAndAuthorizationComeFromDurableData() {
        User newUser = registration.register("New Demo", "  NEW@example.test  ", "Example123!", Role.CUSTOMER);
        assertTrue(newUser.getId() >= 1000);
        assertNotEquals(newUser.getId(), registration.register("Another", "another@example.test", "Example123!", Role.CUSTOMER).getId());
        assertEquals(newUser.getId(), new JdbcUserRepository(jdbc).findByEmail("new@example.test").getId());
        assertThrows(IllegalArgumentException.class,
            () -> registration.register("Duplicate", "NEW@example.test", "Example123!", Role.CUSTOMER));
        login("staff2@example.test");
        assertThrows(SecurityException.class, () -> restaurantService.pauseRestaurant(1L));
        assertNull(new JdbcRestaurantAssignments(jdbc).get(newUser.getId()));
    }

    @Test void priceSnapshotsAndFinancialHistorySurviveCatalogChanges() {
        Order order = place();
        BigDecimal original = jdbc.queryForObject("SELECT unit_price FROM careeats.order_lines WHERE order_id=?", BigDecimal.class, order.getOrderId());
        tx().executeWithoutResult(status -> {
            DatabaseLock.acquire(jdbc);
            jdbc.update("UPDATE careeats.menu_items SET price=99.99 WHERE restaurant_id=1 AND item_id=1");
        });
        try {
            assertEquals(original, jdbc.queryForObject("SELECT unit_price FROM careeats.order_lines WHERE order_id=?", BigDecimal.class, order.getOrderId()));
            // Explicit PRC fixture: this tests persistence, not a DLV completion workflow.
            Instant placed = Instant.parse("2026-11-01T12:00:00Z");
            DeliveredOrder delivered = new DeliveredOrder(order.getOrderId(), 1L, 1L, 3L,
                List.of(new PricedOrderLine(1L, 1, original)), placed, placed.plusSeconds(1800));
            OrderSummary summary = pricing.recordDeliveredOrder(delivered);
            assertThrows(IllegalStateException.class, () -> pricing.recordDeliveredOrder(delivered));
            assertEquals(summary.fare().totalCharge(), jdbc.queryForObject(
                "SELECT total_charge FROM careeats.fare_snapshots WHERE order_id=?", BigDecimal.class, order.getOrderId()));
            User manager = users.findByEmail("restaurant@careeats.com"); // user 2, restaurant 1
            assertTrue(pricing.historyFor(manager).stream().anyMatch(e -> e.orderId().equals(order.getOrderId())));
            assertTrue(pricing.historyFor(users.findByEmail("staff2@example.test")).stream().noneMatch(e -> e.orderId().equals(order.getOrderId())));
            pricing.refundDeliveredOrder(users.findByEmail("operator@careeats.com"), order.getOrderId());
            assertThrows(IllegalStateException.class, () -> pricing.refundDeliveredOrder(users.findByEmail("operator@careeats.com"), order.getOrderId()));
            assertEquals(2, new JdbcLedgerRepository(jdbc).findByOrderId(order.getOrderId()).size());
        } finally {
            tx().executeWithoutResult(status -> {
                DatabaseLock.acquire(jdbc);
                jdbc.update("UPDATE careeats.menu_items SET price=? WHERE restaurant_id=1 AND item_id=1", original);
            });
        }
    }

    @Test void failedMutationsRollBackAndDoNotResetRestaurantHistory() {
        login("restaurant@careeats.com");
        restaurantService.pauseRestaurant(1L);
        Restaurant paused = new JdbcRestaurantRepository(jdbc).findById(1L);
        String event = paused.getTransitionHistory().get(paused.getTransitionHistory().size() - 1).getEventId();
        assertThrows(IllegalStateException.class, () -> restaurantService.pauseRestaurant(1L));
        restaurantService.reopenRestaurant(1L);
        assertTrue(restaurants.findById(1L).getTransitionHistory().stream().anyMatch(e -> e.getEventId().equals(event)));
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM careeats.orders", Long.class);
        login("customer@careeats.com");
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(1L, List.of(new OrderLine(999L, 1))));
        assertEquals(count, jdbc.queryForObject("SELECT COUNT(*) FROM careeats.orders", Long.class));
    }

    @Test void financialWritesRollbackOnPriceMismatchAndSerializeAcrossInstances() throws Exception {
        Order order = place();
        BigDecimal price = jdbc.queryForObject("SELECT unit_price FROM careeats.order_lines WHERE order_id=?", BigDecimal.class, order.getOrderId());
        Instant placed = Instant.parse("2026-11-01T12:00:00Z");
        DeliveredOrder invalid = new DeliveredOrder(order.getOrderId(), 1L, 1L, 3L,
            List.of(new PricedOrderLine(1L, 1, new BigDecimal("0.01"))), placed, placed.plusSeconds(1800));
        assertThrows(IllegalArgumentException.class, () -> pricing.recordDeliveredOrder(invalid));
        assertTrue(ledger.findByOrderId(order.getOrderId()).isEmpty());
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM careeats.delivered_orders WHERE order_id=?", Long.class, order.getOrderId()));

        DeliveredOrder valid = new DeliveredOrder(order.getOrderId(), 1L, 1L, 3L,
            List.of(new PricedOrderLine(1L, 1, price)), placed, placed.plusSeconds(1800));
        Callable<Boolean> charge = () -> {
            PricingService fresh = new PricingService(new PricingCalculator(new PricingRules(
                new BigDecimal("3.99"), new BigDecimal("0.12"), new BigDecimal("20.00"), new BigDecimal("2.99"))),
                ledger, Clock.systemUTC(), new JdbcRestaurantAssignments(jdbc));
            try {
                tx().execute(status -> fresh.recordDeliveredOrder(valid));
                return true;
            } catch (IllegalStateException duplicate) { return false; }
        };
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            var results = workers.invokeAll(List.of(charge, charge));
            long successful = 0;
            for (var result : results) if (result.get(20, TimeUnit.SECONDS)) successful++;
            assertEquals(1L, successful);
            assertEquals(1, ledger.findByOrderId(order.getOrderId()).size());
        } finally { workers.shutdownNow(); }
    }
}
