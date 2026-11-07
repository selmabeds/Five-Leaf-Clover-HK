package com.careeats.backend.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Fail at startup rather than serve requests against an unmigrated or wrong database. */
@Component
@Order(-100)
public class DatabaseStartupCheck implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public DatabaseStartupCheck(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void run(ApplicationArguments args) {
        try {
            jdbc.queryForObject("SELECT id FROM careeats.write_lock WHERE id=1", Integer.class);
            for (String table : new String[]{"app_users", "restaurants", "restaurant_assignments", "menu_items",
                    "menu_ingredients", "orders", "order_lines", "order_transitions", "restaurant_transitions",
                    "menu_transitions", "delivered_orders", "ledger_entries", "fare_snapshots", "priced_lines", "seed_runs"}) {
                jdbc.queryForObject("SELECT COUNT(*) FROM careeats." + table + " WHERE 1=0", Long.class);
            }
        } catch (org.springframework.dao.DataAccessException error) {
            throw new IllegalStateException("CARE Eats schema is unavailable. Check the database target and apply the reviewed Flyway migrations; see backend/DATABASE.md.", error);
        }
    }
}
