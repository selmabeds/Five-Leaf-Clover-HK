package com.careeats.backend.dash;

import com.careeats.backend.ord.application.MenuService;
import com.careeats.backend.ord.application.OrderService;
import com.careeats.backend.ord.application.RestaurantService;
import com.careeats.backend.ord.domain.Order;
import com.careeats.backend.ord.domain.OrderLine;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashController {

    private final RestaurantService restaurantService;
    private final MenuService menuService;
    private final OrderService orderService;

    public DashController(
            RestaurantService restaurantService,
            MenuService menuService,
            OrderService orderService) {

        this.restaurantService = restaurantService;
        this.menuService = menuService;
        this.orderService = orderService;
    }

    @GetMapping("/restaurants")
    public Object getRestaurants() {
        return restaurantService.getAllRestaurants();
    }

    @GetMapping("/restaurants/{id}/menu")
    public Object getMenu(@PathVariable Long id) {
        return menuService.getMenu(id);
    }

    public record PlaceOrderRequest(
            Long restaurantId,
            List<OrderLine> orderLines) {}

    @PostMapping("/orders")
    public Order placeOrder(@RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(
            request.restaurantId(),
            request.orderLines()
        );
    }

    @GetMapping("/orders/customer")
    public List<Order> getCustomerOrders() {
        return orderService.getCustomerOrders();
    }

    @GetMapping("/restaurants/{id}/orders")
    public List<Order> getRestaurantOrders(@PathVariable Long id) {
        return orderService.getRestaurantOrders(id);
    }

    @PostMapping("/orders/{id}/accept")
    public Order acceptOrder(@PathVariable Long id) {
        return orderService.acceptOrder(id);
    }

    public record RejectRequest(String reason) {}

    @PostMapping("/orders/{id}/reject")
    public Order rejectOrder(
            @PathVariable Long id,
            @RequestBody RejectRequest request) {

        return orderService.rejectOrder(id, request.reason());
    }

    @PostMapping("/restaurants/{id}/start-next")
    public Order startNextOrder(@PathVariable Long id) {
        return orderService.startNextOrder(id);
    }

    @PostMapping("/orders/{id}/ready")
    public Order markReady(@PathVariable Long id) {
        return orderService.markOrderReady(id);
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        IllegalStateException.class
    })
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidRequest(Exception error) {
        return Map.of("error", error.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
    public Map<String, String> handleForbidden(SecurityException error) {
        return Map.of("error", error.getMessage());
    }
}

