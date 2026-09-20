package com.digitallifetwin.auth.exception;

public class AccountLinkingRequiredException extends RuntimeException {

    public AccountLinkingRequiredException() {
        super("An account already exists for this email. Sign in with your password.");
    }
}
