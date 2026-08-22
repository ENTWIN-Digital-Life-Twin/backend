package com.digitallifetwin.wellness.exception;

public class MealNotFoundException extends RuntimeException {

    public MealNotFoundException() {
        super("Meal not found");
    }
}
