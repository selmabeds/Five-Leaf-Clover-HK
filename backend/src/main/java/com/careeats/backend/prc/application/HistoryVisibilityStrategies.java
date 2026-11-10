
package com.careeats.backend.prc.application;

import com.careeats.backend.reg.domain.Role;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Selects the strategy matching the authenticated user's role. */
public final class HistoryVisibilityStrategies {
    private final Map<Role, HistoryVisibilityStrategy> byRole = new EnumMap<>(Role.class);

    public HistoryVisibilityStrategies(List<HistoryVisibilityStrategy> strategies) {
        for (HistoryVisibilityStrategy strategy : strategies) {
            if (byRole.putIfAbsent(strategy.role(), strategy) != null) {
                throw new IllegalArgumentException("Duplicate history visibility strategy: " + strategy.role());
            }
        }
        for (Role role : Role.values()) {
            if (!byRole.containsKey(role)) {
                throw new IllegalArgumentException("Missing history visibility strategy: " + role);
            }
        }
    }

    public HistoryVisibilityStrategy forRole(Role role) {
        HistoryVisibilityStrategy strategy = byRole.get(role);
        if (strategy == null) throw new SecurityException("Unsupported user role");
        return strategy;
    }
}
