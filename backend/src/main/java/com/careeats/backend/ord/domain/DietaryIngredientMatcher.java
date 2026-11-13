package com.careeats.backend.ord.domain;

import com.careeats.backend.reg.domain.DietaryRestriction;
import com.careeats.backend.ord.infrastructure.DemoCatalog;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class DietaryIngredientMatcher {
    private static final Set<String> KNOWN_INGREDIENTS = DemoCatalog.create().findAll().stream()
        .flatMap(restaurant -> restaurant.getMenuItems().stream())
        .flatMap(item -> item.getIngredients().stream())
        .map(DietaryIngredientMatcher::normalize)
        .collect(Collectors.toUnmodifiableSet());

    private DietaryIngredientMatcher() {}

    public static boolean matches(List<String> ingredients, Set<DietaryRestriction> restrictions) {
        if (ingredients.isEmpty()) return false;

        List<String> normalized = ingredients.stream().map(DietaryIngredientMatcher::normalize).toList();
        if (!KNOWN_INGREDIENTS.containsAll(normalized)) return false;

        return restrictions.stream().allMatch(restriction -> matches(normalized, restriction));
    }

    private static boolean matches(List<String> ingredients, DietaryRestriction restriction) {
        boolean meat = ingredients.stream().anyMatch(DietaryIngredientMatcher::isMeat);
        boolean fish = ingredients.contains("salmon");
        boolean egg = ingredients.contains("egg");
        boolean dairy = ingredients.stream().anyMatch(ingredient ->
            ingredient.equals("butter") || ingredient.startsWith("mozzarella (")
                || ingredient.startsWith("cheddar ("));

        return switch (restriction) {
            case VEGAN -> !meat && !fish && !egg && !dairy;
            case VEGETARIAN -> !meat && !fish;
            case PESCATARIAN -> !meat;
            case HALAL -> meat && ingredients.stream().filter(DietaryIngredientMatcher::isMeat)
                .allMatch(ingredient -> ingredient.startsWith("halal "));
            case GLUTEN_FREE -> ingredients.stream().noneMatch(ingredient ->
                ingredient.startsWith("wheat ") || ingredient.startsWith("oat milk"));
            case DAIRY_FREE, LACTOSE_FREE -> !dairy;
            case FISH_FREE -> !fish;
            case EGG_FREE -> !egg;
            case SOY_FREE -> ingredients.stream().noneMatch(ingredient -> ingredient.startsWith("tofu ("));
            case SESAME_FREE -> !ingredients.contains("sesame seeds");
            // These allergens are absent from the explicitly defined fictional recipes.
            case PEANUT_FREE, TREE_NUT_FREE, SHELLFISH_FREE -> true;
            // Ingredients alone do not establish certification or nutrient amounts.
            case KOSHER, LOW_SODIUM, LOW_SUGAR, LOW_CARB -> false;
        };
    }

    private static boolean isMeat(String ingredient) {
        return ingredient.equals("chicken") || ingredient.equals("halal chicken")
            || ingredient.equals("halal beef");
    }

    private static String normalize(String ingredient) {
        return ingredient.trim().toLowerCase(Locale.ROOT);
    }
}
