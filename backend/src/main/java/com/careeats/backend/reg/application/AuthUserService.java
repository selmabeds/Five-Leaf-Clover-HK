package com.careeats.backend.reg.application;

import org.springframework.security.core.userdetails.UserDetails; //account info needed for authentification
import org.springframework.security.core.userdetails.UserDetailsService; //retive accounts
import org.springframework.security.core.userdetails.UsernameNotFoundException; //in case account doesnt exist

import com.careeats.backend.reg.infrastructure.UserRepo;
import com.careeats.backend.reg.domain.User;

public class AuthUserService implements UserDetailsService {

    private final UserRepo userRepo;

    public AuthUserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {

        User account = userRepo.findByEmail(email);

        if (account == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(account.getEmail())
                .password(account.getPasswordHash())
                .roles(account.getRole().name())
                .build();
    }
}
