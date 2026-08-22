package com.digitallifetwin.wellness.dto.request;

import com.digitallifetwin.wellness.enums.GoalStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateGoalStatusRequest(
        @NotNull GoalStatus status
) {
}
