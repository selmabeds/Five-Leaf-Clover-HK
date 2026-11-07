package com.careeats.backend.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

/** Live read-only view: services must not copy this into a stale authorization cache. */
public class JdbcRestaurantAssignments extends AbstractMap<Long, Long> {
    private final JdbcTemplate jdbc;
    public JdbcRestaurantAssignments(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public Long get(Object key) {
        if (!(key instanceof Long)) return null;
        return jdbc.query("SELECT restaurant_id FROM careeats.restaurant_assignments WHERE user_id=?",
            (rs,n) -> rs.getLong(1),key).stream().findFirst().orElse(null);
    }
    @Override public Set<Entry<Long, Long>> entrySet() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(jdbc.query(
            "SELECT user_id,restaurant_id FROM careeats.restaurant_assignments ORDER BY user_id",
            (rs,n) -> Map.entry(rs.getLong(1),rs.getLong(2)))));
    }
}
