package com.careeats.backend.persistence;

import com.careeats.backend.reg.domain.*;
import com.careeats.backend.reg.infrastructure.UserRepo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
public class JdbcUserRepository extends UserRepo {
    private final JdbcTemplate jdbc;
    public JdbcUserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public long nextId() { return jdbc.queryForObject("SELECT nextval('careeats.user_ids')", Long.class); }
    @Override public void lockForWrite() { DatabaseLock.acquire(jdbc); }


    @Override public void save(User user) {
    Objects.requireNonNull(user);
    lockForWrite();

    // Save the user's basic account information
    jdbc.update(
        "INSERT INTO careeats.app_users(id,name,email,password_hash,role) VALUES (?,?,?,?,?)",
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getRole().name()
    );

    // Save each dietary restriction associated with the customer
    for (DietaryRestriction restriction : user.getDietaryRestrictions()) {
        jdbc.update(
            "INSERT INTO careeats.customer_dietary_restrictions(user_id,restriction) VALUES (?,?)",
            user.getId(),
            restriction.name()
        );
    }
}




    @Override public User findByEmail(String email) {

    // Step 1: Find the user in the database
    User user = jdbc.query(
        "SELECT * FROM careeats.app_users WHERE email=?",
        (rs, n) -> new User(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            Role.valueOf(rs.getString("role"))
        ),
        email
    ).stream().findFirst().orElse(null);

    // Step 2: If the user does not exist, return null
    if (user == null) {
        return null;
    }

    // Step 3: Retrieve the user's dietary restrictions
    Set<DietaryRestriction> restrictions = jdbc.queryForList(
        "SELECT restriction FROM careeats.customer_dietary_restrictions WHERE user_id=?",
        String.class,
        user.getId()
    ).stream()
     .map(DietaryRestriction::valueOf)
     .collect(Collectors.toSet());

    // Step 4: Add the dietary restrictions to the User object
    if (user.getRole() == Role.CUSTOMER) {
        user.updateDietaryRestrictions(restrictions);
    }

    // Step 5: Return the complete user
    return user;
}



    @Override public boolean emailExists(String email) { return findByEmail(email) != null; }

    @Override
    @Transactional public void updateDietaryRestrictions(
        Long userId,
        Set<DietaryRestriction> restrictions) {

    Objects.requireNonNull(userId, "User ID is required");

    Set<DietaryRestriction> updatedRestrictions =
            restrictions == null ? Set.of() : Set.copyOf(restrictions);

    // Verify that the account belongs to a customer
    String role = jdbc.query(
        "SELECT role FROM careeats.app_users WHERE id=?",
        rs -> rs.next() ? rs.getString("role") : null,
        userId
    );

    if (!Role.CUSTOMER.name().equals(role)) {
        throw new IllegalArgumentException(
            "Only existing customers can update dietary restrictions"
        );
    }

    // Remove previously saved restrictions
    jdbc.update(
        "DELETE FROM careeats.customer_dietary_restrictions WHERE user_id=?",
        userId
    );

    // Save the new selections
    for (DietaryRestriction restriction : updatedRestrictions) {
        jdbc.update(
            "INSERT INTO careeats.customer_dietary_restrictions(user_id,restriction) VALUES (?,?)",
            userId,
            restriction.name()
        );
    }
}

}
