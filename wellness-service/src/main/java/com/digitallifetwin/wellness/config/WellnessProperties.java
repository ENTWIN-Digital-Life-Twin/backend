package com.digitallifetwin.wellness.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wellness")
public record WellnessProperties(
        String defaultTimezone,
        int defaultWaterGoalMl
) {

    public ZoneId zoneId() {
        String tz = defaultTimezone != null && !defaultTimezone.isBlank()
                ? defaultTimezone
                : "Africa/Casablanca";
        return ZoneId.of(tz);
    }

    public int waterGoalMl() {
        return defaultWaterGoalMl > 0 ? defaultWaterGoalMl : 2000;
    }
}
