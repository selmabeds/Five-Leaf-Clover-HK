package com.careeats.backend.reg.infrastructure;

import com.careeats.backend.reg.domain.User;

import java.util.HashMap;
import java.util.Map;

import com.careeats.backend.reg.domain.DietaryRestriction;
import java.util.Set;

public class UserRepo {

    private final java.util.concurrent.atomic.AtomicLong ids = new java.util.concurrent.atomic.AtomicLong(1);
    public long nextId() { return ids.getAndIncrement(); }
    public void lockForWrite() { }

    private final Map<String, User> users = new HashMap<>();

    //stores user, and thier email is the key
    public void save(User user) {
        users.put(user.getEmail(), user);
    }

    //find user using email (key)
    public User findByEmail(String email) {
        return users.get(email);
    }

    //true if the email exists as a key, false otherwise
    public boolean emailExists(String email) {
        return users.containsKey(email);
    }

    public void updateDietaryRestrictions(
        Long userId,
        Set<DietaryRestriction> restrictions) {

    User user = users.values().stream()
        .filter(u -> u.getId().equals(userId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "User not found"
        ));

    user.updateDietaryRestrictions(restrictions);
}
}
