package com.digitallifetwin.notification.entity;

import com.digitallifetwin.notification.enums.RecurrenceType;
import com.digitallifetwin.notification.enums.ReminderSourceType;
import com.digitallifetwin.notification.enums.ReminderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", length = 4000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 40)
    private ReminderType reminderType;

    @Column(name = "trigger_date_time", nullable = false)
    private Instant triggerDateTime;

    @Column(name = "recurring", nullable = false)
    private boolean recurring;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 20)
    private RecurrenceType recurrenceType = RecurrenceType.NONE;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 40)
    private ReminderSourceType sourceType;

    @Column(name = "source_resource_id")
    private UUID sourceResourceId;

    @Column(name = "advance_minutes")
    private Integer advanceMinutes;

    @Column(name = "next_trigger_at", nullable = false)
    private Instant nextTriggerAt;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        deleted = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void softDelete() {
        deleted = true;
        deletedAt = Instant.now();
        enabled = false;
    }
}
