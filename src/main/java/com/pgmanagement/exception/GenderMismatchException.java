package com.pgmanagement.exception;

/**
 * Thrown when user's gender doesn't match PG's gender preference
 * HTTP Status: 400 BAD REQUEST
 */
public class GenderMismatchException extends RuntimeException {

    public GenderMismatchException(String pgGenderPreference, String userGender) {
        super(String.format("This PG is for %s only. Your gender: %s",
                pgGenderPreference, userGender));
    }

    public GenderMismatchException(String message) {
        super(message);
    }
}
