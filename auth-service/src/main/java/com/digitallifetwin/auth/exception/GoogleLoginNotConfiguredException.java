package com.digitallifetwin.auth.exception;

public class GoogleLoginNotConfiguredException extends RuntimeException {

    public GoogleLoginNotConfiguredException() {
        super("Google sign-in is not configured");
    }
}
