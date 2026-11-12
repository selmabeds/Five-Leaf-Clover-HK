
package com.careeats.backend.reg.presentation;

import com.careeats.backend.reg.application.ProfileService;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.domain.DietaryRestriction;

import java.security.Principal;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Retrieve the logged-in customer's profile
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(
            Principal principal) {

        User user = profileService.getProfile(
            principal.getName()
        );

        return ResponseEntity.ok(toResponse(user));
    }

    // Update dietary restrictions
    @PutMapping("/dietary-restrictions")
    public ResponseEntity<ProfileResponse> updateRestrictions(
            Principal principal,
            @RequestBody UpdateRestrictionsRequest request) {

        User user = profileService.updateDietaryRestrictions(
            principal.getName(),
            request.dietaryRestrictions()
        );

        return ResponseEntity.ok(toResponse(user));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(
            user.getName(),
            user.getEmail(),
            user.getDietaryRestrictions()
        );
    }

    public record ProfileResponse(
        String name,
        String email,
        Set<DietaryRestriction> dietaryRestrictions
    ) {}

    public record UpdateRestrictionsRequest(
        Set<DietaryRestriction> dietaryRestrictions
    ) {}


@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleInvalidRequest(
        IllegalArgumentException e) {

    return ResponseEntity.badRequest().body(e.getMessage());
}

}
