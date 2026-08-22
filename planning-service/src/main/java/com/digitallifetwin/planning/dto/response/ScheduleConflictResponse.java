package com.digitallifetwin.planning.dto.response;

import java.util.UUID;

public record ScheduleConflictResponse(
        String conflictType,
        UUID conflictingResourceId,
        String conflictingResourceType,
        String message
) {
}
