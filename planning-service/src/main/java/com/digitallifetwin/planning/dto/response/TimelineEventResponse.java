package com.digitallifetwin.planning.dto.response;

public record TimelineEventResponse(
        String time,
        String title,
        String detail,
        String type
) {
}
