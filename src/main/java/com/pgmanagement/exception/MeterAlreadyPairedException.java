package com.pgmanagement.exception;

/**
 * Thrown when attempting to pair a meter that is already paired
 * HTTP Status: 409 CONFLICT
 */
public class MeterAlreadyPairedException extends RuntimeException {

    public MeterAlreadyPairedException(String message) {
        super(message);
    }

    public MeterAlreadyPairedException(String meterId, Long roomId) {
        super(String.format("Meter '%s' is already paired to room ID: %d", meterId, roomId));
    }
}
