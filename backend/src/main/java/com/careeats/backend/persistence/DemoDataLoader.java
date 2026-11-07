package com.careeats.backend.persistence;

import com.careeats.backend.ord.domain.*;
import com.careeats.backend.ord.infrastructure.DemoCatalog;
import com.careeats.backend.ord.infrastructure.OrderRepository;
import com.careeats.backend.ord.infrastructure.RestaurantRepository;
import com.careeats.backend.reg.domain.*;
import com.careeats.backend.reg.infrastructure.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.*;
import java.util.*;

/** Explicit opt-in only. One transaction and a durable marker prevent repeat/concurrent resets. */
@Component
@ConditionalOnProperty(name = "careeats.seed.enabled", havingValue = "true")
public class DemoDataLoader implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final UserRepo users;
    private final RestaurantRepository restaurants;
    private final OrderRepository orders;
    private final PasswordEncoder passwords;

    public DemoDataLoader(JdbcTemplate jdbc, UserRepo users, RestaurantRepository restaurants,
                          OrderRepository orders, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.users = users;
        this.restaurants = restaurants;
        this.orders = orders;
        this.passwords = passwords;
    }

    public record SeedAccount(Long id, String name, String email, String password, String role) { }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        DatabaseLock.acquire(jdbc);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM careeats.seed_runs WHERE name='demo-v1'", Long.class) > 0) return;

        // Fail rather than partially seed into existing live data or change somebody's account.
        long existing = jdbc.queryForObject("SELECT COUNT(*) FROM careeats.app_users", Long.class)
                      + jdbc.queryForObject("SELECT COUNT(*) FROM careeats.restaurants", Long.class)
                      + jdbc.queryForObject("SELECT COUNT(*) FROM careeats.orders", Long.class);
        if (existing > 0) throw new IllegalStateException(
            "Demo seeding requires an empty CARE Eats database or an existing demo-v1 marker. Disable the seeder; existing data was not changed.");

        List<SeedAccount> accounts;
        try (var input = new ClassPathResource("accounts.json").getInputStream()) {
            accounts = new ObjectMapper().readValue(input, new TypeReference<List<SeedAccount>>() { });
        }
        for (SeedAccount account : accounts) {
            users.save(new User(account.id(), account.name(), account.email(),
                passwords.encode(account.password()), Role.valueOf(account.role())));
        }
        for (Restaurant restaurant : DemoCatalog.create().findAll()) restaurants.save(restaurant);

        // Published synthetic demo credentials only; never use these accounts for real data.
        String demoHash = passwords.encode("DemoOnly123!");
        List<String> names = List.of("Amira", "Noah", "Lina", "Ethan", "Sofia", "Omar",
            "Mila", "Leo", "Aya", "Adam", "Nora", "Samir", "Chloe", "Hugo", "Ines", "Yanis",
            "Emma", "Louis", "Sara", "Elias", "Jade", "Rayan", "Alice", "Malik");
        for (int i = 0; i < names.size(); i++) {
            users.save(new User(100L + i, names.get(i) + " Demo", "customer" + (i + 1) + "@example.test", demoHash, Role.CUSTOMER));
        }
        assign(2L, 1L);
        for (long restaurantId = 2; restaurantId <= 14; restaurantId++) {
            long userId = 200 + restaurantId;
            users.save(new User(userId, "Restaurant " + restaurantId + " Staff", "staff" + restaurantId + "@example.test", demoHash, Role.RESTAURANT));
            assign(userId, restaurantId);
        }
        for (long i = 0; i < 5; i++) users.save(new User(300L + i, "Courier " + (i + 1),
            "courier" + (i + 1) + "@example.test", demoHash, Role.COURIER));

        LocalDateTime start = LocalDateTime.now(Clock.systemUTC()).minusDays(14).withNano(0);
        OrderStatus[] targets = {OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING,
            OrderStatus.READY, OrderStatus.REJECTED, OrderStatus.CANCELLED};
        int index = 0;
        for (long restaurantId = 1; restaurantId <= 14; restaurantId++) {
            Restaurant restaurant = restaurants.findById(restaurantId);
            List<MenuItem> available = restaurant.getMenuItems().stream()
                .filter(item -> item.getStatus() == MenuItemStatus.AVAILABLE).toList();
            for (OrderStatus target : targets) {
                long id = 100L + index;
                MenuItem item = available.get(index % available.size());
                List<OrderLine> lines = List.of(new OrderLine(item.getItemId(), 1 + index % 3));
                boolean terminal = target == OrderStatus.REJECTED || target == OrderStatus.CANCELLED;
                LocalDateTime placed = terminal ? start.plusHours(index * 3L)
                    : LocalDateTime.now(Clock.systemUTC()).withNano(0).minusMinutes(60 + index);
                List<StateTransitionRecord> history = history(id, target, placed);
                orders.save(Order.restore(id, 100L + index % 24, restaurantId, lines, target,
                    target == OrderStatus.REJECTED ? "Kitchen capacity reached" : null, history));
                index++;
            }
        }
        jdbc.update("INSERT INTO careeats.seed_runs(name,completed_at) VALUES ('demo-v1',?)", OffsetDateTime.now(ZoneOffset.UTC));
    }

    private void assign(long userId, long restaurantId) {
        jdbc.update("INSERT INTO careeats.restaurant_assignments(user_id,restaurant_id) VALUES (?,?)", userId, restaurantId);
    }

    private List<StateTransitionRecord> history(long orderId, OrderStatus target, LocalDateTime placed) {
        List<OrderStatus> states = new ArrayList<>(List.of(OrderStatus.PLACED));
        if (target == OrderStatus.REJECTED || target == OrderStatus.CANCELLED) states.add(target);
        else if (target != OrderStatus.PLACED) {
            states.add(OrderStatus.ACCEPTED);
            if (target == OrderStatus.PREPARING || target == OrderStatus.READY) states.add(OrderStatus.PREPARING);
            if (target == OrderStatus.READY) states.add(OrderStatus.READY);
        }
        List<StateTransitionRecord> history = new ArrayList<>();
        for (int i = 0; i < states.size(); i++) history.add(new StateTransitionRecord(
            UUID.randomUUID().toString(), orderId, i == 0 ? null : states.get(i - 1), states.get(i), placed.plusMinutes(i * 5L)));
        return history;
    }
}
