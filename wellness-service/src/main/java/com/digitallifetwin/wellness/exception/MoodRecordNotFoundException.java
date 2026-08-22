package com.digitallifetwin.wellness.exception;

public class MoodRecordNotFoundException extends RuntimeException {

    public MoodRecordNotFoundException() {
        super("Mood record not found");
    }
}
