package com.hosannasolutions.solisla.user;

import java.security.SecureRandom;

/** One-time credential generation for accounts provisioned on someone else's behalf (e.g. the
 *  platform bootstrap admin) rather than via self-service signup — see PlatformBootstrapSeeder. */
public final class TemporaryPasswordGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}
