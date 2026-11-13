package com.careeats.backend.persistence;

import com.careeats.backend.ord.domain.MenuItem;
import com.careeats.backend.ord.domain.Restaurant;
import com.careeats.backend.ord.infrastructure.DemoCatalog;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DASH Reset (Phase III 5.4 "The Reset action that returns the system to its initial state";
 * Spec DASH "Reset Seed: restore the initial state for repeatable demos"; R-DASH-01 operator
 * "restore the initial state").
 *
 * The initial state is the R-ORD-01 configuration of the city: restaurants with their status,
 * menu items with their availability, couriers offline at their starting position, and no orders.
 * Accounts, staff assignments and dietary restrictions are never touched (R-REG-03 seeded accounts
 * stay as they are).
 *
 * Safeguard: only allowed on the local demo database (careeats.seed.enabled=true), so a Reset
 * can never wipe the team's shared database.
 */
@Service
public class DemoResetService {
    private final JdbcTemplate jdbc;
    private final UserRepo users;
    private final boolean seedEnabled;

    public DemoResetService(JdbcTemplate jdbc, UserRepo users,
                            @Value("${careeats.seed.enabled:false}") boolean seedEnabled) {
        this.jdbc = jdbc;
        this.users = users;
        this.seedEnabled = seedEnabled;
    }

    @Transactional
    public void resetToInitialState() {
        requireOperator();

        if (!seedEnabled) {
            throw new IllegalStateException(
                "Reset is only available on the local demo database (CAREEATS_SEED_ENABLED=true); the shared database was not changed.");
        }

        // Same application-wide write lock as every other write
        DatabaseLock.acquire(jdbc);

        // No orders in the initial configuration: remove orders and everything attached to them (child tables first)
        jdbc.update("DELETE FROM careeats.ledger_entries");
        jdbc.update("DELETE FROM careeats.fare_snapshots");
        jdbc.update("DELETE FROM careeats.priced_lines");
        jdbc.update("DELETE FROM careeats.delivered_orders");
        jdbc.update("DELETE FROM careeats.delivery_assignments");
        jdbc.update("DELETE FROM careeats.order_transitions");
        jdbc.update("DELETE FROM careeats.order_lines");
        jdbc.update("DELETE FROM careeats.orders");

        // Couriers: without a saved row, DLV treats a courier as OFFLINE at its starting position
        jdbc.update("DELETE FROM careeats.couriers");

        // The initial state has no restaurant / menu status changes yet
        jdbc.update("DELETE FROM careeats.restaurant_transitions");
        jdbc.update("DELETE FROM careeats.menu_transitions");

        // Restaurant status and menu availability back to the configured values (R-ORD-01)
        for (Restaurant restaurant : DemoCatalog.create().findAll()) {
            jdbc.update("UPDATE careeats.restaurants SET status=? WHERE id=?",
                restaurant.getStatus().name(), restaurant.getRestaurantId());

            for (MenuItem item : restaurant.getMenuItems()) {
                jdbc.update("UPDATE careeats.menu_items SET status=? WHERE restaurant_id=? AND item_id=?",
                    item.getStatus().name(), restaurant.getRestaurantId(), item.getItemId());
            }
        }
    }

    // R-DASH-01: only the operator may restore the initial state
    private void requireOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new SecurityException("User is not authenticated");
        }

        User user = users.findByEmail(authentication.getName());

        if (user == null || user.getRole() != Role.OPERATOR) {
            throw new SecurityException("Only the operator can reset the system");
        }
    }
}
