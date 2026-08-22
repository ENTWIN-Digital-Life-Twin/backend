package com.digitallifetwin.planning.exception;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException() {
        super("Calendar event not found");
    }
}
