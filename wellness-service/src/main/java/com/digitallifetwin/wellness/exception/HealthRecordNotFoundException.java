package com.digitallifetwin.wellness.exception;

public class HealthRecordNotFoundException extends RuntimeException {

    public HealthRecordNotFoundException() {
        super("Health record not found");
    }
}
