package com.careeats.backend.reg.infrastructure;

import com.careeats.backend.reg.application.RegistrationService;
import com.careeats.backend.reg.domain.Role;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class LoadingAccounts implements CommandLineRunner {

    private final RegistrationService registrationService;

    public LoadingAccounts(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Override
    public void run(String... args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        List<SeedAccount> accounts = mapper.readValue(
            new ClassPathResource("accounts.json").getInputStream(),
            new TypeReference<List<SeedAccount>>() {}
        );

        for (SeedAccount account : accounts) {
            registrationService.register(
                account.name(),
                account.email(),
                account.password(),
                Role.valueOf(account.role())
            );
        }
    }

    public record SeedAccount(
        String name,
        String email,
        String password,
        String role
    ) {}
}
