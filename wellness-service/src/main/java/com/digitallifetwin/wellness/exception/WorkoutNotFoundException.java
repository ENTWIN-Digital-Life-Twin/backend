package com.digitallifetwin.wellness.exception;

public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException() {
        super("Workout not found");
    }
}
