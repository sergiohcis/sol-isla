package com.hosannasolutions.solisla.config;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fires only under the {@code bootstrap} profile — never on ordinary startup, prod included.
 * Run once, on purpose: start the backend with {@code SPRING_PROFILES_ACTIVE=bootstrap} for a
 * single run (e.g. {@code java -jar backend.jar --spring.profiles.active=bootstrap}), read the
 * generated password from this log line, then go back to running without the {@code bootstrap}
 * profile — re-running it is harmless (idempotent) but won't recover a lost password.
 */
@Component
@Profile("bootstrap")
public class PlatformBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformBootstrapRunner.class);

    private final PlatformBootstrapSeeder platformBootstrapSeeder;

    public PlatformBootstrapRunner(PlatformBootstrapSeeder platformBootstrapSeeder) {
        this.platformBootstrapSeeder = platformBootstrapSeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<String> temporaryPassword = platformBootstrapSeeder.seed();
        if (temporaryPassword.isPresent()) {
            log.info("Bootstrapped admin account — log in as {} with temporary password: {}",
                    PlatformBootstrapSeeder.OWNER_EMAIL, temporaryPassword.get());
        } else {
            log.info("Bootstrap account {} already exists — nothing to do.", PlatformBootstrapSeeder.OWNER_EMAIL);
        }
    }
}
