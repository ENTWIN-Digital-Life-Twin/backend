package com.digitallifetwin.wellness.service;

import com.digitallifetwin.wellness.enums.GoalStatus;
import com.digitallifetwin.wellness.exception.InvalidGoalTransitionException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class GoalStatusTransitions {

    private static final Map<GoalStatus, Set<GoalStatus>> ALLOWED = new EnumMap<>(GoalStatus.class);

    static {
        ALLOWED.put(GoalStatus.ACTIVE, EnumSet.of(GoalStatus.COMPLETED, GoalStatus.CANCELLED));
        ALLOWED.put(GoalStatus.CANCELLED, EnumSet.of(GoalStatus.ACTIVE));
        ALLOWED.put(GoalStatus.COMPLETED, EnumSet.of(GoalStatus.ACTIVE));
    }

    private GoalStatusTransitions() {
    }

    public static void assertAllowed(GoalStatus from, GoalStatus to) {
        if (from == to) {
            return;
        }
        Set<GoalStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(GoalStatus.class));
        if (!allowed.contains(to)) {
            throw new InvalidGoalTransitionException(
                    "Invalid goal status transition from " + from + " to " + to);
        }
    }
}
