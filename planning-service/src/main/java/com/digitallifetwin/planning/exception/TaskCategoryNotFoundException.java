package com.digitallifetwin.planning.exception;

public class TaskCategoryNotFoundException extends RuntimeException {

    public TaskCategoryNotFoundException() {
        super("Task category not found");
    }

    public TaskCategoryNotFoundException(String message) {
        super(message);
    }
}
