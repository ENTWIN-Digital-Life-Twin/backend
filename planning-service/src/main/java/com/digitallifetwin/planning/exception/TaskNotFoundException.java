package com.digitallifetwin.planning.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException() {
        super("Task not found");
    }
}
