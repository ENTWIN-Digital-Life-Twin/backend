package com.digitallifetwin.planning.service;

import com.digitallifetwin.planning.enums.TaskStatus;
import com.digitallifetwin.planning.exception.InvalidTaskStateTransitionException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class TaskStatusTransitions {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = new EnumMap<>(TaskStatus.class);

    static {
        ALLOWED.put(TaskStatus.DRAFT, EnumSet.of(
                TaskStatus.SCHEDULED, TaskStatus.COMPLETED, TaskStatus.CANCELLED));
        ALLOWED.put(TaskStatus.SCHEDULED, EnumSet.of(
                TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.OVERDUE, TaskStatus.DRAFT));
        ALLOWED.put(TaskStatus.IN_PROGRESS, EnumSet.of(
                TaskStatus.PAUSED, TaskStatus.COMPLETED, TaskStatus.CANCELLED));
        ALLOWED.put(TaskStatus.PAUSED, EnumSet.of(
                TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED, TaskStatus.COMPLETED));
        ALLOWED.put(TaskStatus.OVERDUE, EnumSet.of(
                TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED, TaskStatus.COMPLETED));
        ALLOWED.put(TaskStatus.COMPLETED, EnumSet.of(
                TaskStatus.SCHEDULED, TaskStatus.IN_PROGRESS, TaskStatus.DRAFT));
        ALLOWED.put(TaskStatus.CANCELLED, EnumSet.noneOf(TaskStatus.class));
    }

    private TaskStatusTransitions() {
    }

    public static void assertAllowed(TaskStatus from, TaskStatus to) {
        if (from == to) {
            return;
        }
        Set<TaskStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(TaskStatus.class));
        if (!allowed.contains(to)) {
            throw new InvalidTaskStateTransitionException(
                    "Invalid task status transition from " + from + " to " + to);
        }
    }
}
