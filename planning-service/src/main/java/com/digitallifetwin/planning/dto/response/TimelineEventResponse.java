package com.digitallifetwin.planning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventResponse {
    private String time;
    private String title;
    private String detail;
    private String type; // work, personal, meeting, break
}
