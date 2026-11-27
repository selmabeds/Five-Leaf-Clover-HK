package com.careeats.backend.rvw.domain;

import java.math.BigDecimal;

public record RestaurantRating(long restaurantId, long reviewCount, BigDecimal averageRating) {}
