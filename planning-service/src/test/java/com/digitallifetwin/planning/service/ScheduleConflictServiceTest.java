package com.digitallifetwin.planning.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.digitallifetwin.planning.service.ScheduleConflictService.TimeInterval;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleConflictServiceTest {

    @Test
    void overlaps_detectsIntersection() {
        Instant aStart = Instant.parse("2026-08-23T10:00:00Z");
        Instant aEnd = Instant.parse("2026-08-23T11:00:00Z");
        Instant bStart = Instant.parse("2026-08-23T10:30:00Z");
        Instant bEnd = Instant.parse("2026-08-23T11:30:00Z");

        assertThat(ScheduleConflictService.overlaps(aStart, aEnd, bStart, bEnd)).isTrue();
    }

    @Test
    void overlaps_nonOverlapping() {
        Instant aStart = Instant.parse("2026-08-23T10:00:00Z");
        Instant aEnd = Instant.parse("2026-08-23T11:00:00Z");
        Instant bStart = Instant.parse("2026-08-23T11:00:00Z");
        Instant bEnd = Instant.parse("2026-08-23T12:00:00Z");

        assertThat(ScheduleConflictService.overlaps(aStart, aEnd, bStart, bEnd)).isFalse();
    }

    @Test
    void occupiedMinutes_doesNotDoubleCountOverlaps() {
        Instant windowStart = Instant.parse("2026-08-23T08:00:00Z");
        Instant windowEnd = Instant.parse("2026-08-23T12:00:00Z");
        List<TimeInterval> intervals = List.of(
                new TimeInterval(
                        Instant.parse("2026-08-23T09:00:00Z"),
                        Instant.parse("2026-08-23T10:00:00Z")),
                new TimeInterval(
                        Instant.parse("2026-08-23T09:30:00Z"),
                        Instant.parse("2026-08-23T10:30:00Z"))
        );

        int occupied = ScheduleConflictService.occupiedMinutes(intervals, windowStart, windowEnd);

        assertThat(occupied).isEqualTo(90);
    }
}
