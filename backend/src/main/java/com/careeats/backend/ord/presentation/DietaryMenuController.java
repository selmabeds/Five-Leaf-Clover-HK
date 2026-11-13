package com.careeats.backend.ord.presentation;

import com.careeats.backend.ord.application.DietaryMenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class DietaryMenuController {
    private final DietaryMenuService menus;

    public DietaryMenuController(DietaryMenuService menus) {
        this.menus = menus;
    }

    @GetMapping("/api/restaurants/for-you")
    public DietaryMenuService.Recommendations forYou(Principal principal) {
        return menus.forCustomer(principal.getName());
    }
}
