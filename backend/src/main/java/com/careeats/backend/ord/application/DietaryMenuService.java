package com.careeats.backend.ord.application;

import com.careeats.backend.ord.domain.MenuItem;
import com.careeats.backend.ord.domain.MenuItemStatus;
import com.careeats.backend.ord.domain.DietaryIngredientMatcher;
import com.careeats.backend.ord.infrastructure.RestaurantRepository;
import com.careeats.backend.reg.application.ProfileService;
import com.careeats.backend.reg.domain.DietaryRestriction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class DietaryMenuService {
    private final RestaurantRepository restaurants;
    private final ProfileService profiles;
    public DietaryMenuService(RestaurantRepository restaurants, ProfileService profiles) {
        this.restaurants = restaurants;
        this.profiles = profiles;
    }

    public Recommendations forCustomer(String email) {
        Set<DietaryRestriction> restrictions = profiles.getProfile(email).getDietaryRestrictions();
        Map<Long, List<MenuItem>> menus = new HashMap<>();
        if (restrictions.isEmpty()) {
            return new Recommendations(restrictions, menus);
        }

        for (var restaurant : restaurants.findAll()) {
            Long restaurantId = restaurant.getRestaurantId();
            List<MenuItem> matches = restaurant.getMenuItems().stream()
                .filter(item -> item.getStatus() == MenuItemStatus.AVAILABLE)
                .filter(item -> DietaryIngredientMatcher.matches(item.getIngredients(), restrictions))
                .toList();
            if (!matches.isEmpty()) {
                menus.put(restaurantId, matches);
            }
        }

        return new Recommendations(restrictions, menus);
    }

    public record Recommendations(Set<DietaryRestriction> dietaryRestrictions,
                                  Map<Long, List<MenuItem>> menus) {}
}
