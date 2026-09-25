package com.digitallifetwin.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.digitallifetwin.notification.enums.RecurrenceType;
import com.digitallifetwin.notification.exception.InvalidReminderConfigurationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class ReminderRecurrenceTest {

    @Test
    void initialNextTrigger_subtractsAdvanceMinutes() {
        Instant trigger = Instant.parse("2026-08-24T10:00:00Z");
        assertThat(ReminderRecurrence.initialNextTrigger(trigger, 15))
                .isEqualTo(Instant.parse("2026-08-24T09:45:00Z"));
        assertThat(ReminderRecurrence.initialNextTrigger(trigger, null)).isEqualTo(trigger);
    }

    @Test
    void daily_advancesOneDay() {
        Instant current = Instant.parse("2026-08-24T10:00:00Z");
        assertThat(ReminderRecurrence.advance(current, RecurrenceType.DAILY))
                .isEqualTo(current.plus(1, ChronoUnit.DAYS));
    }

    @Test
    void weekly_advancesSevenDays() {
        Instant current = Instant.parse("2026-08-24T10:00:00Z");
        assertThat(ReminderRecurrence.advance(current, RecurrenceType.WEEKLY))
                .isEqualTo(current.plus(7, ChronoUnit.DAYS));
    }

    @Test
    void resolve_recurringRequiresType() {
        assertThatThrownBy(() -> ReminderRecurrence.resolve(true, null))
                .isInstanceOf(InvalidReminderConfigurationException.class);
        assertThatThrownBy(() -> ReminderRecurrence.resolve(true, RecurrenceType.NONE))
                .isInstanceOf(InvalidReminderConfigurationException.class);
        assertThat(ReminderRecurrence.resolve(true, RecurrenceType.DAILY)).isEqualTo(RecurrenceType.DAILY);
        assertThat(ReminderRecurrence.resolve(false, null)).isEqualTo(RecurrenceType.NONE);
    }
}
