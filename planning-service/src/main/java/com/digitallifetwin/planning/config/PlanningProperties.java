package com.digitallifetwin.planning.config;

import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planning")
public record PlanningProperties(
        String defaultTimezone,
        LocalTime dayStart,
        LocalTime dayEnd,
        double overloadThresholdRatio
) {

    public ZoneId zoneId() {
        return ZoneId.of(defaultTimezone);
    }
}
