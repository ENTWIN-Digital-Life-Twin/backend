package com.digitallifetwin.notification.scheduler;

import com.digitallifetwin.notification.service.ReminderExecutionService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderExecutionService reminderExecutionService;

    @Scheduled(fixedDelayString = "${notification.scheduler.fixed-delay-ms:60000}")
    public void processDueReminders() {
        int processed = reminderExecutionService.processDueReminders(Instant.now());
        if (processed > 0) {
            log.info("Processed {} due reminder(s)", processed);
        }
    }
}
