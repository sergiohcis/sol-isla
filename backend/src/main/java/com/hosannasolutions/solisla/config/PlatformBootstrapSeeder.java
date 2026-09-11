package com.hosannasolutions.solisla.config;

import com.hosannasolutions.solisla.security.authorization.Role;
import com.hosannasolutions.solisla.user.TemporaryPasswordGenerator;
import com.hosannasolutions.solisla.user.dto.CreateUserRequest;
import com.hosannasolutions.solisla.user.providedService.UserService;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the store owner's own admin account — a one-time, manually-triggered action (see
 * {@link PlatformBootstrapRunner}), never automatic on ordinary startup: this isn't a Flyway
 * migration precisely so it never becomes permanent history silently re-applied to
 * {@code sol_isla_prod}. Mirrors SweetHome's {@code PlatformBootstrapSeeder}.
 */
@Component
public class PlatformBootstrapSeeder {

    public static final String OWNER_EMAIL = "sergiohcis@gmail.com";

    private final UserService userService;

    public PlatformBootstrapSeeder(UserService userService) {
        this.userService = userService;
    }

    /** Returns the generated temporary password only the first time the account is created —
     *  empty on a re-run against an already-bootstrapped instance, since the real password hash
     *  is never re-readable. *///8G3hbJSWFqns9Qm4
    @Transactional
    public Optional<String> seed() {
        if (userService.findByEmail(OWNER_EMAIL).isPresent()) {
            return Optional.empty();
        }
        String temporaryPassword = TemporaryPasswordGenerator.generate();
        userService.createUser(new CreateUserRequest(OWNER_EMAIL, temporaryPassword, "Sergio", "Hernandez Cisneros", Role.ADMIN));
        return Optional.of(temporaryPassword);
    }
}
