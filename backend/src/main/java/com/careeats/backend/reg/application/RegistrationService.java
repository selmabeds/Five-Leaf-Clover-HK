package com.careeats.backend.reg.application;

import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.infrastructure.UserRepo;
import com.careeats.backend.reg.domain.DietaryRestriction;
import java.util.Set;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//to securely store passwords

@org.springframework.transaction.annotation.Transactional
public class RegistrationService {

    private UserRepo userRepo;
    private BCryptPasswordEncoder passwordEncoder;

    public RegistrationService(UserRepo userRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // Registration without dietary restrictions
    public User register(String name, String email, String password, Role role) {
      return register(name, email, password, role, Set.of());
  }

     // Registration with dietary restrictions
    public User register(String name, String email, String password, Role role, Set<DietaryRestriction> dietaryRestrictions) {

        email = validateAndNormalizeInput(name, email, password, role);

        Set<DietaryRestriction> restrictions =
                validateDietaryRestrictions(role, dietaryRestrictions);

        return createAndSaveUser(
                name, email, password, role, restrictions
        );
    }

    // Validate required fields and normalize email
    private String validateAndNormalizeInput(
            String name, String email, String password, Role role) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        email = email.trim().toLowerCase(java.util.Locale.ROOT);

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        return email;
    }

    // Validate dietary restrictions
    private Set<DietaryRestriction> validateDietaryRestrictions(
            Role role, Set<DietaryRestriction> dietaryRestrictions) {

        Set<DietaryRestriction> restrictions =
                dietaryRestrictions == null
                        ? Set.of()
                        : Set.copyOf(dietaryRestrictions);

        if (role != Role.CUSTOMER && !restrictions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Dietary restrictions are only allowed for customers"
            );
        }

        return restrictions;
    }

    // Check duplicate email, hash password and save user
    private User createAndSaveUser(
            String name, String email, String password,
            Role role, Set<DietaryRestriction> restrictions) {

        userRepo.lockForWrite();

        if (userRepo.emailExists(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        String passwordHash = passwordEncoder.encode(password);

        User user = new User(
                userRepo.nextId(),
                name,
                email,
                passwordHash,
                role,
                restrictions
        );

        userRepo.save(user);

        return user;
    }

}
