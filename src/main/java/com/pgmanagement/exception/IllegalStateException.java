package com.pgmanagement.exception;

public class IllegalStateException extends Exception{

    public IllegalStateException(String billAlreadyPaid) {
        super(billAlreadyPaid);
    }
}
