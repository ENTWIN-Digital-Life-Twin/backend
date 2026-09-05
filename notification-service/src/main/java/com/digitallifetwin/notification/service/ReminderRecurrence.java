package com.digitallifetwin.notification.service;

import com.digitallifetwin.notification.enums.RecurrenceType;
import com.digitallifetwin.notification.exception.InvalidReminderConfigurationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class ReminderRecurrence {

    private ReminderRecurrence() {
    }

    public static Instant initialNextTrigger(Instant triggerDateTime, Integer advanceMinutes) {
        int advance = advanceMinutes == null ? 0 : advanceMinutes;
        if (advance < 0) {
            throw new InvalidReminderConfigurationException("advanceMinutes must be >= 0");
        }
        return triggerDateTime.minus(advance, ChronoUnit.MINUTES);
    }

    public static Instant advance(Instant currentNextTrigger, RecurrenceType recurrenceType) {
        return switch (recurrenceType) {
            case DAILY -> currentNextTrigger.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> currentNextTrigger.plus(7, ChronoUnit.DAYS);
            case NONE -> throw new InvalidReminderConfigurationException(
                    "Cannot advance a non-recurring reminder");
        };
    }

    public static RecurrenceType resolve(boolean recurring, RecurrenceType requested) {
        if (recurring) {
            if (requested == null || requested == RecurrenceType.NONE) {
                throw new InvalidReminderConfigurationException(
                        "recurrenceType is required when recurring is true (DAILY or WEEKLY)");
            }
            return requested;
        }
        if (requested != null && requested != RecurrenceType.NONE) {
            throw new InvalidReminderConfigurationException(
                    "recurrenceType must be NONE when recurring is false");
        }
        return RecurrenceType.NONE;
    }
}
