package com.digitallifetwin.planning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingEventResponse {
    private String time;
    private String title;
    private String location;
    private Boolean isOnline;
    private List<String> participants;
}
