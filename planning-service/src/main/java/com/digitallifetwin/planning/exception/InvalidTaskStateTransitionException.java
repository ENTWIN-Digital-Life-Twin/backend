package com.digitallifetwin.planning.exception;

public class InvalidTaskStateTransitionException extends RuntimeException {

    public InvalidTaskStateTransitionException(String message) {
        super(message);
    }
}
