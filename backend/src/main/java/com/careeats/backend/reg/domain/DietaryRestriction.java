package com.careeats.backend.reg.domain;

public enum DietaryRestriction {
    // Food allergies
    PEANUT_FREE,
    TREE_NUT_FREE,
    SHELLFISH_FREE,
    FISH_FREE,
    EGG_FREE,
    SOY_FREE,
    SESAME_FREE,

    // Food intolerances and sensitivities
    GLUTEN_FREE,
    DAIRY_FREE,
    LACTOSE_FREE,

    // Other dietary preferences
    LOW_SODIUM,
    LOW_SUGAR,
    LOW_CARB,

     // Dietary preferences
    VEGETARIAN,
    VEGAN,
    PESCATARIAN,

    // Religious dietary requirements
    HALAL,
    KOSHER
}