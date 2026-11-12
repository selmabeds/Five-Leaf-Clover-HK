
package com.careeats.backend.reg.application;

import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.DietaryRestriction;
import com.careeats.backend.reg.infrastructure.UserRepo;

import java.util.Set;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

public class ProfileService {

    private final UserRepo userRepo;

    public ProfileService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    // Retrieve the logged-in customer's profile
    @Transactional(readOnly = true)
    public User getProfile(String email) {

        User user = userRepo.findByEmail(email);

        if (user == null) {
            throw new IllegalArgumentException(
                "User not found"
            );
        }

        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException(
                "Only customers can access this profile"
            );
        }

        return user;
    }

    // Update the logged-in customer's restrictions
    @Transactional
    public User updateDietaryRestrictions(
            String email,
            Set<DietaryRestriction> restrictions) {

       if (restrictions == null) {
        throw new IllegalArgumentException(
        "Dietary restrictions are required"
    );
}

        // Find the authenticated customer
        User user = getProfile(email);

        // Save the restrictions in the repository
        userRepo.updateDietaryRestrictions(
            user.getId(),
            restrictions
        );

        // Return the updated profile
        return getProfile(email);
    }
}
