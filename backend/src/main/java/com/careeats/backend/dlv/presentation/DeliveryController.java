package com.careeats.backend.dlv.presentation;

import com.careeats.backend.dlv.application.DeliveryService;
import com.careeats.backend.dlv.application.DeliveryService.CourierView;
import com.careeats.backend.dlv.application.DeliveryService.DispatchView;
import com.careeats.backend.dlv.domain.Courier;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {
    private final DeliveryService delivery;

    public DeliveryController(DeliveryService delivery) {
        this.delivery = delivery;
    }

    @GetMapping("/me")
    public CourierView currentCourier() {
        return delivery.currentCourier();
    }

    @GetMapping("/couriers")
    public List<Courier> onDutyCouriers() {
        return delivery.onDutyCouriers();
    }

    @PostMapping("/on-duty")
    public CourierView goOnDuty() {
        return delivery.goOnDuty();
    }

    @PostMapping("/off-duty")
    public CourierView goOffDuty() {
        return delivery.goOffDuty();
    }

    @PostMapping("/orders/{id}/accept")
    public CourierView acceptOffer(@PathVariable long id) {
        return delivery.acceptOffer(id);
    }

    @PostMapping("/orders/{id}/decline")
    public CourierView declineOffer(@PathVariable long id) {
        return delivery.declineOffer(id);
    }

    @PostMapping("/orders/{id}/pickup")
    public CourierView confirmPickup(@PathVariable long id) {
        return delivery.confirmPickup(id);
    }

    @PostMapping("/orders/{id}/deliver")
    public CourierView confirmDelivery(@PathVariable long id) {
        return delivery.confirmDelivery(id);
    }

    @GetMapping("/dispatch")
    public DispatchView dispatchQueue() {
        return delivery.dispatchView();
    }

    @PostMapping("/orders/{id}/reassign")
    public DispatchView reassignStalled(@PathVariable long id) {
        return delivery.reassignStalled(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidRequest(IllegalArgumentException error) {
        return Map.of("error", error.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> invalidState(IllegalStateException error) {
        return Map.of("error", error.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> forbidden(SecurityException error) {
        return Map.of("error", error.getMessage());
    }
}