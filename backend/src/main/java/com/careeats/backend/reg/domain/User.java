package com.careeats.backend.reg.domain;

import java.util.Set;
import java.util.Objects;

public class User {


private Long id;
private String name;
private String email;
private String passwordHash;
private Role role;
private Set<DietaryRestriction> dietaryRestrictions;

// Original constructor (without dietary restrictions)
public User(Long id, String name, String email,
            String passwordHash, Role role) {

    this(id, name, email, passwordHash, role, Set.of());
}

// New constructor (with dietary restrictions)
public User(Long id, String name, String email,
            String passwordHash, Role role,
            Set<DietaryRestriction> dietaryRestrictions) {

    this.id = id;
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = Objects.requireNonNull(role);

    this.dietaryRestrictions = dietaryRestrictions == null
            ? Set.of()
            : Set.copyOf(dietaryRestrictions);

    if (role != Role.CUSTOMER && !this.dietaryRestrictions.isEmpty()) {
        throw new IllegalArgumentException(
            "Dietary restrictions are only allowed for customers"
        );
    }
}


    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Set<DietaryRestriction> getDietaryRestrictions() {
    return dietaryRestrictions;
}

public void updateDietaryRestrictions(
        Set<DietaryRestriction> restrictions) {

    if (role != Role.CUSTOMER) {
        throw new IllegalStateException(
            "Only customers can update dietary restrictions"
        );
    }

    this.dietaryRestrictions = restrictions == null
            ? Set.of()
            : Set.copyOf(restrictions);
}
}
