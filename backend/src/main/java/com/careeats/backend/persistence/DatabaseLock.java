package com.careeats.backend.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** One transaction-scoped mutex shared by all application writers across backend instances. */
public final class DatabaseLock {
    private DatabaseLock() { }
    public static void acquire(JdbcTemplate jdbc) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Database writes require an application transaction");
        }
        jdbc.queryForObject("SELECT id FROM careeats.write_lock WHERE id=1 FOR UPDATE", Integer.class);
    }
}
