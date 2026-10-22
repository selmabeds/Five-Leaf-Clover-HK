
package com.careeats.backend.ord.infrastructure;

import com.careeats.backend.ord.domain.Restaurant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestaurantRepository {

    private final Map<Long, Restaurant> restaurants = new HashMap<>();

    public void save(Restaurant restaurant) {

        if (restaurant == null) {
            throw new IllegalArgumentException(
                    "Restaurant cannot be null"
            );
        }

        restaurants.put(restaurant.getRestaurantId(), restaurant);
    }

    public Restaurant findById(Long restaurantId) {
        return restaurants.get(restaurantId);
    }

    public List<Restaurant> findAll() {
        return new ArrayList<>(restaurants.values());
    }

    public boolean existsById(Long restaurantId) {
        return restaurants.containsKey(restaurantId);
    }
}
