package com.digitallifetwin.wellness.exception;

public class WaterRecordNotFoundException extends RuntimeException {

    public WaterRecordNotFoundException() {
        super("Water record not found");
    }
}
