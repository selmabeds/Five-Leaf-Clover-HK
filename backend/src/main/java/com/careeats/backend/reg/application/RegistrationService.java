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

    public User register(String name, String email, String password, Role role, Set<DietaryRestriction> dietaryRestrictions) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        //
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


       // Validate dietary restrictions
        Set<DietaryRestriction> restrictions = dietaryRestrictions == null
        ? Set.of()
        : Set.copyOf(dietaryRestrictions);

       if (role != Role.CUSTOMER && !restrictions.isEmpty()) {
        throw new IllegalArgumentException(
        "Dietary restrictions are only allowed for customers"
        );
       }

         userRepo.lockForWrite();

        //two accounts can't have the same email
        if (userRepo.emailExists(email)) {
            throw new IllegalArgumentException("Email already registered");
        }


        //store password asa secure hash for safety
        String passwordHash = passwordEncoder.encode(password);

        //if passes all validation, create a new user
       User user = new User(userRepo.nextId(), name, email, passwordHash,role, restrictions);

        userRepo.save(user);

        return user;
    }
}
