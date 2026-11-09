package com.careeats.backend.prc.presentation;

import com.careeats.backend.ord.domain.OrderStatus;
import com.careeats.backend.prc.application.PricingService;
import com.careeats.backend.prc.domain.DeliveredOrder;
import com.careeats.backend.prc.domain.LedgerEntryType;
import com.careeats.backend.prc.domain.PricedOrderLine;
import com.careeats.backend.prc.domain.PricingCalculator;
import com.careeats.backend.prc.domain.PricingRules;
import com.careeats.backend.prc.infrastructure.LedgerRepository;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingControllerTest {

    private final UserRepo userRepo = new UserRepo();
    private final LedgerRepository ledgerRepository = new LedgerRepository();
    private final PricingService pricingService = new PricingService(
            new PricingCalculator(new PricingRules(
                    new BigDecimal("3.99"),
                    new BigDecimal("0.12"),
                    new BigDecimal("20.00"),
                    new BigDecimal("2.99")
            )),
            ledgerRepository,
            Clock.fixed(
                    Instant.parse("2026-11-02T18:31:00Z"),
                    ZoneOffset.UTC
            )
    );
    private final PricingController controller =
            new PricingController(pricingService, org.mockito.Mockito.mock(com.careeats.backend.prc.application.OrderHistoryService.class), userRepo);

    @BeforeEach
    void setUpAuthentication() {
        User operator = user(30L, Role.OPERATOR);
        userRepo.save(operator);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        operator.getEmail(),
                        "password",
                        List.of()
                )
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void operatorCanRefundThroughController() {
        pricingService.recordDeliveredOrder(deliveredOrder(1L));

        var refund = controller.refundOrder(1L);

        assertEquals(LedgerEntryType.REFUND, refund.type());
        assertEquals(OrderStatus.REFUNDED, refund.orderState());
    }

    private DeliveredOrder deliveredOrder(Long orderId) {
        return new DeliveredOrder(
                orderId,
                10L,
                20L,
                30L,
                List.of(new PricedOrderLine(
                        100L,
                        1,
                        new BigDecimal("10.00")
                )),
                Instant.parse("2026-11-02T18:00:00Z"),
                Instant.parse("2026-11-02T18:30:00Z")
        );
    }

    private User user(Long id, Role role) {
        return new User(
                id,
                role.name(),
                id + "@test.com",
                "password",
                role
        );
    }
}
