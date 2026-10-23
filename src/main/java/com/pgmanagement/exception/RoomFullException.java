package com.pgmanagement.exception;

/**
 * Thrown when attempting to book a room that's already at full capacity
 * HTTP Status: 409 CONFLICT
 */
public class RoomFullException extends RuntimeException {

    public RoomFullException() {
        super("Room is already at full capacity");
    }

    public RoomFullException(String roomNumber) {
        super(String.format("Room %s is already at full capacity", roomNumber));
    }


}
