package com.careeats.backend.rvw.domain;

import java.time.Instant;

public record Review(long orderId, long restaurantId, long courierId,
                     int restaurantRating, int courierRating, Instant createdAt) {}
