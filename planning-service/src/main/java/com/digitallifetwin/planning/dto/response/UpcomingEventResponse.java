package com.digitallifetwin.planning.dto.response;

import java.util.List;

public record UpcomingEventResponse(
        String time,
        String title,
        String location,
        Boolean isOnline,
        List<String> participants
) {
}
