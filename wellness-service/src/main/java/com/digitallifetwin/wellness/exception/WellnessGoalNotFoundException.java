package com.digitallifetwin.wellness.exception;

public class WellnessGoalNotFoundException extends RuntimeException {

    public WellnessGoalNotFoundException() {
        super("Wellness goal not found");
    }
}
