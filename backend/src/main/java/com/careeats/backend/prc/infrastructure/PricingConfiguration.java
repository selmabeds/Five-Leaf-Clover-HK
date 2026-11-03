package com.careeats.backend.prc.infrastructure;

import com.careeats.backend.prc.domain.PricingCalculator;
import com.careeats.backend.prc.domain.PricingRules;
import com.careeats.backend.prc.application.PricingService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;

@Configuration
public class PricingConfiguration {

    // Reads the pricing values from application.properties
    @Bean
    public PricingRules pricingRules(
            @Value("${prc.pricing.delivery-fee}") BigDecimal deliveryFee,
            @Value("${prc.pricing.service-fee}") BigDecimal serviceFee,
            @Value("${prc.pricing.small-order-threshold}")
            BigDecimal smallOrderThreshold,
            @Value("${prc.pricing.small-order-surcharge}")
            BigDecimal smallOrderSurcharge) {

        return new PricingRules(
                deliveryFee,
                serviceFee,
                smallOrderThreshold,
                smallOrderSurcharge
        );
    }

    // Creates the calculator used by the PRC application service
    @Bean
    public PricingCalculator pricingCalculator(PricingRules pricingRules) {
        return new PricingCalculator(pricingRules);
    }

    // Creates the ledger used to store charges and refunds
    @Bean
    public LedgerRepository ledgerRepository() {
        return new LedgerRepository();
    }

    // Creates the pricing service with the pricing setup reuqirements
    @Bean
    public PricingService pricingService(
            PricingCalculator pricingCalculator,
            LedgerRepository ledgerRepository) {

        return new PricingService(
                pricingCalculator,
                ledgerRepository,
                Clock.systemUTC()
        );
    }
}
