package com.digitallifetwin.planning.dto.request;

import com.digitallifetwin.planning.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull(message = "Status is required")
        TaskStatus status
) {
}
