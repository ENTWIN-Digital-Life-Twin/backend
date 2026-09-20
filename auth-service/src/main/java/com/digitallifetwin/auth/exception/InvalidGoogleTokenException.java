package com.digitallifetwin.auth.exception;

public class InvalidGoogleTokenException extends RuntimeException {

    public InvalidGoogleTokenException() {
        super("Invalid Google credential");
    }
}
