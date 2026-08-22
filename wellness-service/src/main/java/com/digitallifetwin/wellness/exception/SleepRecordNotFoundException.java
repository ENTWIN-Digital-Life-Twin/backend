package com.digitallifetwin.wellness.exception;

public class SleepRecordNotFoundException extends RuntimeException {

    public SleepRecordNotFoundException() {
        super("Sleep record not found");
    }
}
