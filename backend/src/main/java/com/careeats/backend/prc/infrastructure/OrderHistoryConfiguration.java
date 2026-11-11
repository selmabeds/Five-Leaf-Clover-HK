
package com.careeats.backend.prc.infrastructure;

import com.careeats.backend.prc.application.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@Configuration
public class OrderHistoryConfiguration {
    @Bean public OrderHistoryRepository orderHistoryRepository(JdbcTemplate jdbc) {
        return new JdbcOrderHistoryRepository(jdbc);
    }

    @Bean public HistoryVisibilityStrategies historyVisibilityStrategies() {
        return new HistoryVisibilityStrategies(List.of(
            new CustomerHistoryStrategy(),
            new RestaurantHistoryStrategy(),
            new OperatorHistoryStrategy(),
            new CourierHistoryStrategy()
        ));
    }

    @Bean public OrderHistoryService orderHistoryService(OrderHistoryRepository orders,
        LedgerRepository ledger, Map<Long, Long> restaurantAssignments,
        HistoryVisibilityStrategies visibilityStrategies) {
        return new OrderHistoryService(orders, ledger, restaurantAssignments, visibilityStrategies);
    }

    @Bean public DeliveryBillingService deliveryBillingService(OrderHistoryRepository orders,
        LedgerRepository ledger, PricingService pricing) {
        return new DeliveryBillingService(orders, ledger, pricing);
    }
}
