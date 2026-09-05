package com.digitallifetwin.notification.exception;

public class ReminderNotFoundException extends RuntimeException {

    public ReminderNotFoundException() {
        super("Reminder not found");
    }
}
