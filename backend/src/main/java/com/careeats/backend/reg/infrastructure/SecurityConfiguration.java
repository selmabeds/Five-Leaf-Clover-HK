package com.careeats.backend.reg.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.careeats.backend.reg.application.AuthUserService;
import com.careeats.backend.reg.application.RegistrationService;

@Configuration
public class SecurityConfiguration {

    @Bean
    public UserRepo userRepo() {
        return new UserRepo();
    }

    @Bean
    public AuthUserService authUserService(UserRepo userRepo) {
        return new AuthUserService(userRepo);
    }

    @Bean
    public RegistrationService registrationService(UserRepo userRepo) {
        return new RegistrationService(userRepo);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/register").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .successHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.getWriter().write("Login successful");
                })
                .failureHandler((request, response, exception) -> {
                    response.sendError(401, "Invalid email or password");
                })
                .permitAll()
)
            .logout(logout -> logout
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.getWriter().write("Logout successful");
                })
                .permitAll()
            );

        return http.build();
    }
}
