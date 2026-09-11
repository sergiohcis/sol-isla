package com.hosannasolutions.solisla.user.exception;

public class DuplicateUserEmailException extends RuntimeException {

    public DuplicateUserEmailException(String email) {
        super("A user with this email already exists: " + email);
    }
}
