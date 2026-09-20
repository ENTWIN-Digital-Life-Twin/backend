package com.digitallifetwin.auth.exception;

public class MailNotSentException extends RuntimeException {

    public MailNotSentException() {
        super("Could not send the verification email. Try again in a moment.");
    }
}
