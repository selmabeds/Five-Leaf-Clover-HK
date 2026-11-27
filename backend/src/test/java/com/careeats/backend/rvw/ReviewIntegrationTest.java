package com.careeats.backend.rvw;

import com.careeats.backend.dlv.application.DeliveryService;
import com.careeats.backend.ord.application.OrderService;
import com.careeats.backend.ord.domain.OrderLine;
import com.careeats.backend.persistence.DemoResetService;
import com.careeats.backend.prc.application.PricingService;
import com.careeats.backend.reg.infrastructure.UserRepo;
import com.careeats.backend.rvw.application.ReviewService;
import com.careeats.backend.rvw.infrastructure.ReviewRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:rvw-integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.flyway.enabled=true", "careeats.seed.enabled=true"
})
class ReviewIntegrationTest {
    @Autowired ReviewService reviews;
    @Autowired OrderService orders;
    @Autowired DeliveryService deliveries;
    @Autowired DemoResetService reset;
    @Autowired PricingService pricing;
    @Autowired UserRepo users;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void startFromReset() {
        login("operator@careeats.com");
        reset.resetToInitialState();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void clearIdentity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deliveryReviewDuplicateRefusalAndAverageFollowTheRealFlow() throws Exception {
        long first = deliver("customer1@example.test", 1);
        login("customer1@example.test");
        var saved = reviews.submit(first, 5, 4);
        assertEquals(3, saved.courierId());
        assertEquals(first, saved.orderId());
        assertEquals(409, assertThrows(ResponseStatusException.class,
            () -> reviews.submit(first, 1, 1)).getStatusCode().value());

        long second = deliver("customer2@example.test", 1);
        login("customer2@example.test");
        reviews.submit(second, 2, 5);
        assertEquals(new BigDecimal("3.50"), reviews.restaurantRating(1).averageRating());
        assertEquals(2, reviews.restaurantRating(1).reviewCount());
        assertEquals(1, reviews.myReviews().size());
        assertEquals(second, reviews.myReviews().get(0).orderId());
        mvc.perform(get("/api/reviews/restaurants/1").with(user("operator@careeats.com")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.averageRating").value(3.5))
            .andExpect(jsonPath("$.reviewCount").value(2));
    }

    @Test
    void rejectsUndeliveredAndOtherCustomersOrdersAndNonCustomerRoles() {
        login("customer1@example.test");
        long pending = orders.placeOrder(1L, List.of(new OrderLine(1L, 1))).getOrderId();
        assertEquals(409, assertThrows(ResponseStatusException.class,
            () -> reviews.submit(pending, 5, 5)).getStatusCode().value());
        orders.cancelOrder(pending);
        assertEquals(409, assertThrows(ResponseStatusException.class,
            () -> reviews.submit(pending, 5, 5)).getStatusCode().value());

        long delivered = deliver("customer1@example.test", 1);
        login("customer2@example.test");
        assertThrows(SecurityException.class, () -> reviews.submit(delivered, 5, 5));
        for (String email : List.of("restaurant@careeats.com", "courier@careeats.com", "operator@careeats.com")) {
            login(email);
            assertThrows(SecurityException.class, () -> reviews.submit(delivered, 5, 5));
        }
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM careeats.order_reviews", Long.class));
    }

    @Test
    void endpointRequiresSessionCsrfAndWholeScoresWithinRange() throws Exception {
        long orderId = deliver("customer1@example.test", 1);
        String path = "/api/reviews/orders/" + orderId;
        clearIdentity();
        mvc.perform(post(path).with(csrf()).contentType("application/json")
            .content("{\"restaurantRating\":5,\"courierRating\":4}")).andExpect(status().isUnauthorized());
        mvc.perform(post(path).with(user("customer1@example.test")).contentType("application/json")
            .content("{\"restaurantRating\":5,\"courierRating\":4}")).andExpect(status().isForbidden());
        for (String content : List.of(
            "{\"restaurantRating\":0,\"courierRating\":4}",
            "{\"restaurantRating\":6,\"courierRating\":4}",
            "{\"restaurantRating\":4.5,\"courierRating\":4}",
            "{\"restaurantRating\":4,\"courierRating\":null}")) {
            mvc.perform(post(path).with(user("customer1@example.test")).with(csrf())
                .contentType("application/json").content(content)).andExpect(status().isBadRequest());
        }
        mvc.perform(post(path).with(user("customer1@example.test")).with(csrf()).contentType("application/json")
            .content("{\"restaurantRating\":5,\"courierRating\":4}")).andExpect(status().isCreated());
        mvc.perform(post(path).with(user("customer1@example.test")).with(csrf()).contentType("application/json")
            .content("{\"restaurantRating\":5,\"courierRating\":4}")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("This order has already been rated"));
        mvc.perform(get("/api/reviews/mine").with(user("customer2@example.test")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void averageIsRestaurantScopedAndReviewsSurviveRepositoryRecreationAndRefund() {
        long first = deliver("customer1@example.test", 1);
        login("customer1@example.test");
        reviews.submit(first, 5, 1);
        long otherRestaurant = deliver("customer2@example.test", 2);
        login("customer2@example.test");
        reviews.submit(otherRestaurant, 1, 5);
        var reloaded = new ReviewRepository(jdbc);
        assertEquals(new BigDecimal("5.00"), reloaded.restaurantRating(1).averageRating());
        assertEquals(new BigDecimal("1.00"), reloaded.restaurantRating(2).averageRating());
        assertEquals(1, reloaded.forCustomer(100).size());
        login("operator@careeats.com");
        pricing.refundDeliveredOrder(users.findByEmail("operator@careeats.com"), first);
        assertEquals(1, reloaded.restaurantRating(1).reviewCount());
        login("customer1@example.test");
        assertThrows(ResponseStatusException.class, () -> reviews.submit(first, 3, 3));
    }

    @Test
    void resetRemovesReviewsAndAllowsANewDemoWithoutDeletingAccounts() {
        long delivered = deliver("customer1@example.test", 1);
        login("customer1@example.test");
        reviews.submit(delivered, 4, 5);
        login("operator@careeats.com");
        reset.resetToInitialState();
        assertEquals(0, reviews.restaurantRating(1).reviewCount());
        assertNull(reviews.restaurantRating(1).averageRating());
        login("customer1@example.test");
        assertTrue(reviews.myReviews().isEmpty());
        long next = deliver("customer1@example.test", 1);
        login("customer1@example.test");
        assertEquals(5, reviews.submit(next, 5, 5).restaurantRating());
    }

    @Test
    void concurrentSubmissionsRecordExactlyOneReview() throws Exception {
        long delivered = deliver("customer1@example.test", 1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> submit = () -> {
            login("customer1@example.test");
            try {
                start.await();
                reviews.submit(delivered, 5, 5);
                return 201;
            } catch (ResponseStatusException conflict) {
                return conflict.getStatusCode().value();
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        try {
            var first = pool.submit(submit);
            var second = pool.submit(submit);
            start.countDown();
            assertEquals(List.of(201, 409), List.of(first.get(10, TimeUnit.SECONDS),
                second.get(10, TimeUnit.SECONDS)).stream().sorted().toList());
            assertEquals(1, reviews.restaurantRating(1).reviewCount());
        } finally {
            pool.shutdownNow();
        }
    }

    private long deliver(String customer, long restaurantId) {
        login(customer);
        long itemId = restaurantId == 1 ? 1 : 3;
        long orderId = orders.placeOrder(restaurantId, List.of(new OrderLine(itemId, 1))).getOrderId();
        login(restaurantId == 1 ? "restaurant@careeats.com" : "staff2@example.test");
        orders.acceptOrder(orderId);
        orders.startNextOrder(restaurantId);
        orders.markOrderReady(orderId);
        login("courier@careeats.com");
        deliveries.goOnDuty();
        deliveries.acceptOffer(orderId);
        deliveries.confirmPickup(orderId);
        deliveries.confirmDelivery(orderId);
        return orderId;
    }

    private void login(String email) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(email, "unused", List.of()));
    }
}
