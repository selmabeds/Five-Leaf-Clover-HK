package com.careeats.backend.reg.application;

import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//to securely store passwords

public class RegistrationService {

    private UserRepo userRepo;
    private BCryptPasswordEncoder passwordEncoder;
    private long nextId = 1L;

    public RegistrationService(UserRepo userRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public User register(String name, String email, String password, Role role) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        //
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        email = email.trim().toLowerCase();

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

         //two accounts can't have the same email
        if (userRepo.emailExists(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        //store password asa secure hash for safety
        String passwordHash = passwordEncoder.encode(password);

        //if passes all validation, create a new user
        User user = new User(nextId++, name, email, passwordHash, role);

        userRepo.save(user);

        return user;
    }
}
