package com.digitallifetwin.notification.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateReminderEnabledRequest(
        @NotNull Boolean enabled
) {
}
