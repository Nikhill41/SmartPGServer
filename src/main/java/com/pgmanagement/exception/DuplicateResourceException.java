package com.pgmanagement.exception;

public class DuplicateResourceException extends Exception{
    public DuplicateResourceException() {
        super("duplicate resource found");
    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}
