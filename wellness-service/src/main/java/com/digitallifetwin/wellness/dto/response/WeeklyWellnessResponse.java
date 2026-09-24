package com.digitallifetwin.wellness.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyWellnessResponse {
    private List<String> labels;      // ["Mon", "Tue", "Wed", ...]
    private List<Integer> sleep;      // [89, 91, 85, ...]
    private List<Integer> activity;   // [72, 80, 68, ...]
    private List<Integer> nutrition;  // [62, 71, 65, ...]
}
