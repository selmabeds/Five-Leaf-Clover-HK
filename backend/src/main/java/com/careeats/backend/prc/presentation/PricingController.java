package com.careeats.backend.prc.presentation;

import com.careeats.backend.prc.application.PricingService;
import com.careeats.backend.prc.domain.LedgerEntry;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prc")
public class PricingController {

    private final PricingService pricingService;
    private final UserRepo userRepo;

    public PricingController(
            PricingService pricingService,
            UserRepo userRepo) {
        this.pricingService = pricingService;
        this.userRepo = userRepo;
    }

    @GetMapping("/history")
    public List<LedgerEntry> getHistory() {
        return pricingService.historyFor(getAuthenticatedUser());
    }

    @PostMapping("/orders/{orderId}/refund")
    public LedgerEntry refundOrder(@PathVariable Long orderId) {
        return pricingService.refundDeliveredOrder(
                getAuthenticatedUser(),
                orderId
        );
    }

    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new SecurityException("User is not authenticated");
        }

        User user = userRepo.findByEmail(authentication.getName());
        if (user == null) {
            throw new SecurityException("Authenticated user not found");
        }

        return user;
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        IllegalStateException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidRequest(Exception error) {
        return Map.of("error", error.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleForbidden(SecurityException error) {
        return Map.of("error", error.getMessage());
    }
}
