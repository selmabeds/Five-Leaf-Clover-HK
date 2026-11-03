package com.careeats.backend.prc.domain;

import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.prc.infrastructure.LedgerRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LedgerRepositoryTest {

    @Test
    void preservesChargeAndRefundAsSeparateEntries() {
        LedgerRepository repository = new LedgerRepository();
        Instant placedAt = Instant.parse("2026-11-02T18:00:00Z");
        Instant deliveredAt = Instant.parse("2026-11-02T18:30:00Z");

        repository.save(new LedgerEntry(
                1L,
                LedgerEntryType.CHARGE,
                OrderStatus.DELIVERED,
                10L,
                20L,
                30L,
                new BigDecimal("19.49"),
                placedAt,
                deliveredAt,
                deliveredAt
        ));
        repository.save(new LedgerEntry(
                1L,
                LedgerEntryType.REFUND,
                OrderStatus.REFUNDED,
                10L,
                20L,
                30L,
                new BigDecimal("19.49"),
                placedAt,
                deliveredAt,
                Instant.parse("2026-11-02T18:35:00Z")
        ));

        assertEquals(2, repository.findByOrderId(1L).size());
        assertEquals(
                LedgerEntryType.REFUND,
                repository.findByOrderId(1L).get(1).type()
        );
    }
}
