package com.pgmanagement.exception;

/**
 * Thrown when attempting to control a meter that is offline
 * HTTP Status: 400 BAD REQUEST
 */
public class MeterOfflineException extends RuntimeException {

//    public MeterOfflineException(String message) {
//        super(message);
//    }

    public MeterOfflineException(String meterId) {
        super(String.format("Meter '%s' is offline and cannot be controlled", meterId));
    }

    public MeterOfflineException(String meterId, long lastSeenMinutesAgo) {
        super(String.format("Meter '%s' is offline (last seen %d minutes ago)", meterId, lastSeenMinutesAgo));
    }
}
