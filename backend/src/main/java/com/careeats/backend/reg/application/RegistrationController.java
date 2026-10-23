package com.careeats.backend.reg.application;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.User;

@RestController
@RequestMapping("/api/register")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            User user = registrationService.register(
                request.name(),
                request.email(),
                request.password(),
                Role.CUSTOMER
            );

            return ResponseEntity.ok("Account created: " + user.getEmail());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public record RegisterRequest(
        String name,
        String email,
        String password
    ) {}
}
