package com.digitallifetwin.auth.exception;

public class GoogleEmailNotVerifiedException extends RuntimeException {

    public GoogleEmailNotVerifiedException() {
        super("Google did not provide a verified email");
    }
}
