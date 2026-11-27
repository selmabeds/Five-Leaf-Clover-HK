package com.careeats.backend.ord.application;

import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

final class AuthenticatedUserProvider {

    private final UserRepo userRepo;

    AuthenticatedUserProvider(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new SecurityException("User is not authenticated");
        }

        User user = userRepo.findByEmail(authentication.getName());

        if (user == null) {
            throw new SecurityException("Authenticated user not found");
        }

        return user;
    }
}
