
package com.careeats.backend.reg.application;

import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.domain.Role;
import com.careeats.backend.reg.domain.DietaryRestriction;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationCharacterizationTest {

    private UserRepo userRepo;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        userRepo = spy(new UserRepo());
        registrationService = new RegistrationService(userRepo);
    }

    // TC1
    @Test
    void validCustomerRegistrationCreatesAndSavesUser() {

        User user = registrationService.register(
                "Test Customer",
                "customer@example.com",
                "password123",
                Role.CUSTOMER
        );

        assertNotNull(user.getId());
        assertEquals("Test Customer", user.getName());
        assertEquals("customer@example.com", user.getEmail());
        assertEquals(Role.CUSTOMER, user.getRole());
        assertTrue(user.getDietaryRestrictions().isEmpty());

        assertSame(
                user,
                userRepo.findByEmail("customer@example.com")
        );

        verify(userRepo).save(user);
    }

    // TC2
    @Test
    void emailIsTrimmedAndConvertedToLowercase() {

        User user = registrationService.register(
                "Test Customer",
                "  CUSTOMER@EXAMPLE.COM  ",
                "password123",
                Role.CUSTOMER
        );

        assertEquals("customer@example.com", user.getEmail());

        assertSame(
                user,
                userRepo.findByEmail("customer@example.com")
        );

        assertNull(
                userRepo.findByEmail("  CUSTOMER@EXAMPLE.COM  ")
        );
    }

    // TC3
    @Test
    void passwordIsStoredAsBCryptHash() {

        String password = "password123";

        User user = registrationService.register(
                "Test Customer",
                "customer@example.com",
                password,
                Role.CUSTOMER
        );

        assertNotEquals(password, user.getPasswordHash());

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        assertTrue(
                encoder.matches(password, user.getPasswordHash())
        );
    }

    // TC4
    @Test
    void nullNameIsRejected() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(
                        null,
                        "customer@example.com",
                        "password123",
                        Role.CUSTOMER
                )
        );

        assertEquals("Name is required", exception.getMessage());

        verify(userRepo, never()).save(any(User.class));
    }

    // TC5
    @Test
    void blankNameIsRejected() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(
                        "   ",
                        "customer@example.com",
                        "password123",
                        Role.CUSTOMER
                )
        );

        assertEquals("Name is required", exception.getMessage());

        verify(userRepo, never()).save(any(User.class));
    }

    // TC6
    @Test
    void nullEmailIsRejected() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(
                        "Test Customer",
                        null,
                        "password123",
                        Role.CUSTOMER
                )
        );

        assertEquals("Email is required", exception.getMessage());

        verify(userRepo, never()).save(any(User.class));
    }

    // TC7
    @Test
    void blankEmailIsRejected() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(
                        "Test Customer",
                        "   ",
                        "password123",
                        Role.CUSTOMER
                )
        );

        assertEquals("Email is required", exception.getMessage());

        verify(userRepo, never()).save(any(User.class));
    }

    // TC8
    @Test
    void nullPasswordIsRejected() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(
                        "Test Customer",
                        "customer@example.com",
                        null,
                        Role.CUSTOMER
                )
        );

        assertEquals("Password is required", exception.getMessage());

        verify(userRepo, never()).save(any(User.class));
    }

    // TC9
    @Test
    void blankPasswordIsRejected() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(
                        "Test Customer",
                        "customer@example.com",
                        "   ",
                        Role.CUSTOMER
                )
        );

        assertEquals("Password is required", exception.getMessage());

        verify(userRepo, never()).save(any(User.class));
    }

    // TC10
    @Test
    void nullRoleIsRejected() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(
                        "Test Customer",
                        "customer@example.com",
                        "password123",
                        null
                )
        );

        assertEquals("Role is required", exception.getMessage());

        verify(userRepo, never()).save(any(User.class));
    }

    // TC11
    @Test
    void duplicateEmailIsRejectedAfterNormalization() {

        User originalUser = registrationService.register(
                "First Customer",
                "customer@example.com",
                "password123",
                Role.CUSTOMER
        );

        clearInvocations(userRepo);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(
                        "Second Customer",
                        "  CUSTOMER@EXAMPLE.COM  ",
                        "anotherPassword",
                        Role.CUSTOMER
                )
        );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        assertSame(
                originalUser,
                userRepo.findByEmail("customer@example.com")
        );

        verify(userRepo, never()).save(any(User.class));
    }

    // TC12
    @Test
    void customerCanRegisterWithDietaryRestrictions() {

        Set<DietaryRestriction> restrictions = Set.of(
                DietaryRestriction.HALAL,
                DietaryRestriction.GLUTEN_FREE
        );

        User user = registrationService.register(
                "Test Customer",
                "customer@example.com",
                "password123",
                Role.CUSTOMER,
                restrictions
        );

        assertEquals(
                restrictions,
                user.getDietaryRestrictions()
        );

        assertEquals(
                restrictions,
                userRepo.findByEmail("customer@example.com")
                        .getDietaryRestrictions()
        );

        verify(userRepo).save(user);
    }

    // TC13
    @Test
    void nullDietaryRestrictionsBecomeEmptySet() {

        User user = registrationService.register(
                "Test Customer",
                "customer@example.com",
                "password123",
                Role.CUSTOMER,
                null
        );

        assertNotNull(user.getDietaryRestrictions());
        assertTrue(user.getDietaryRestrictions().isEmpty());

        verify(userRepo).save(user);
    }

    // TC14
    @Test
    void nonCustomerCannotRegisterWithDietaryRestrictions() {

        for (Role role : Set.of(
                Role.RESTAURANT,
                Role.COURIER,
                Role.OPERATOR
        )) {

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> registrationService.register(
                            "Test User",
                            "user@example.com",
                            "password123",
                            role,
                            Set.of(DietaryRestriction.VEGAN)
                    )
            );

            assertEquals(
                    "Dietary restrictions are only allowed for customers",
                    exception.getMessage()
            );
        }

        verify(userRepo, never()).save(any(User.class));
    }

    // TC15
    @Test
    void nonCustomersCanRegisterWithoutDietaryRestrictions() {

        for (Role role : Set.of(
                Role.RESTAURANT,
                Role.COURIER,
                Role.OPERATOR
        )) {

            User user = registrationService.register(
                    "Test User",
                    role.name() + "@example.com",
                    "password123",
                    role,
                    Set.of()
            );

            assertEquals(role, user.getRole());
            assertTrue(user.getDietaryRestrictions().isEmpty());

            verify(userRepo).save(user);
        }
    }

    // TC16
    @Test
    void nameValidationOccursBeforeOtherFieldValidation() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals("Name is required", exception.getMessage());

        verify(userRepo, never()).save(any(User.class));
    }
}
