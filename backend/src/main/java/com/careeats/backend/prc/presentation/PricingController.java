package com.careeats.backend.prc.presentation;

import com.careeats.backend.prc.application.*;
import com.careeats.backend.prc.domain.*;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prc")
public class PricingController {
    private final PricingService pricing;
    private final OrderHistoryService history;
    private final UserRepo users;
    public PricingController(PricingService pricing,OrderHistoryService history,UserRepo users) {
        this.pricing=pricing; this.history=history; this.users=users;
    }
    @GetMapping("/context") public OrderHistoryService.UserContext getContext() { return history.context(authenticatedUser()); }
    @GetMapping("/history") public List<OrderHistoryItem> getHistory() { return history.historyFor(authenticatedUser()); }
    @GetMapping("/orders/{orderId}") public OrderReceipt getDetails(@PathVariable Long orderId) {
        return history.detailsFor(authenticatedUser(),orderId);
    }
    @GetMapping("/orders/{orderId}/receipt") public OrderReceipt getReceipt(@PathVariable Long orderId) {
        return history.receiptFor(authenticatedUser(),orderId);
    }
    @PostMapping("/orders/{orderId}/refund") public LedgerEntry refundOrder(@PathVariable Long orderId) {
        return pricing.refundDeliveredOrder(authenticatedUser(),orderId);
    }
    private User authenticatedUser() {
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication==null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken)
            throw new SecurityException("Authentication required");
        User user=users.findByEmail(authentication.getName());
        if(user==null) throw new SecurityException("Authenticated user not found");
        return user;
    }
    @ExceptionHandler(PrcNotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String,String> notFound(PrcNotFoundException error) { return Map.of("error",error.getMessage()); }
    @ExceptionHandler(ReceiptUnavailableException.class) @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String,String> notReady(ReceiptUnavailableException error) { return Map.of("error",error.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalid(RuntimeException error) { return Map.of("error",error.getMessage()); }
    @ExceptionHandler(SecurityException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String,String> forbidden(SecurityException error) { return Map.of("error",error.getMessage()); }
}
