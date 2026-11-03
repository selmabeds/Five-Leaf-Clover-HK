package com.careeats.backend.prc.infrastructure;

import com.careeats.backend.prc.domain.LedgerEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LedgerRepository {

    private final Map<Long, List<LedgerEntry>> entriesByOrder =
            new LinkedHashMap<>();

    // Saves a charge or refund entry for an order
    public synchronized void save(LedgerEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Ledger entry cannot be null");
        }

        entriesByOrder
                .computeIfAbsent(entry.orderId(), ignored -> new ArrayList<>())
                .add(entry);
    }

    // Returns all charge and refund entries for one order
    public synchronized List<LedgerEntry> findByOrderId(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is needed");
        }

        return Collections.unmodifiableList(new ArrayList<>(
                entriesByOrder.getOrDefault(orderId, List.of())
        ));
    }

    // Returns every entry currently stored in the ledger
    public synchronized List<LedgerEntry> findAll() {
        return Collections.unmodifiableList(
                entriesByOrder.values().stream()
                        .flatMap(List::stream)
                        .toList()
        );
    }
}
