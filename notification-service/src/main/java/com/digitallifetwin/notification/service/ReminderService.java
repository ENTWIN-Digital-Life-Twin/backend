package com.digitallifetwin.notification.service;

import com.digitallifetwin.notification.dto.request.CreateReminderRequest;
import com.digitallifetwin.notification.dto.request.UpdateReminderEnabledRequest;
import com.digitallifetwin.notification.dto.request.UpdateReminderRequest;
import com.digitallifetwin.notification.dto.response.PageResponse;
import com.digitallifetwin.notification.dto.response.ReminderResponse;
import com.digitallifetwin.notification.entity.Reminder;
import com.digitallifetwin.notification.enums.RecurrenceType;
import com.digitallifetwin.notification.enums.ReminderType;
import com.digitallifetwin.notification.exception.InvalidReminderStateException;
import com.digitallifetwin.notification.exception.ReminderNotFoundException;
import com.digitallifetwin.notification.mapper.NotificationMapper;
import com.digitallifetwin.notification.repository.ReminderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public ReminderResponse create(UUID userId, CreateReminderRequest request) {
        RecurrenceType recurrenceType = ReminderRecurrence.resolve(request.recurring(), request.recurrenceType());

        Reminder reminder = new Reminder();
        reminder.setUserId(userId);
        applyFields(
                reminder,
                request.title(),
                request.message(),
                request.reminderType(),
                request.triggerDateTime(),
                request.recurring(),
                recurrenceType,
                request.enabled() == null || request.enabled(),
                request.sourceType(),
                request.sourceResourceId(),
                request.advanceMinutes()
        );
        reminder.setDeleted(false);
        return notificationMapper.toReminderResponse(reminderRepository.save(reminder));
    }

    @Transactional(readOnly = true)
    public ReminderResponse getById(UUID userId, UUID id) {
        return notificationMapper.toReminderResponse(requireOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReminderResponse> list(
            UUID userId,
            Boolean enabled,
            ReminderType reminderType,
            Instant from,
            Instant to,
            Pageable pageable) {
        Page<Reminder> page = reminderRepository.findAll(
                ReminderRepository.withFilters(userId, enabled, reminderType, from, to), pageable);
        List<ReminderResponse> content = page.getContent().stream()
                .map(notificationMapper::toReminderResponse)
                .toList();
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public ReminderResponse update(UUID userId, UUID id, UpdateReminderRequest request) {
        Reminder reminder = requireOwned(userId, id);
        RecurrenceType recurrenceType = ReminderRecurrence.resolve(request.recurring(), request.recurrenceType());
        applyFields(
                reminder,
                request.title(),
                request.message(),
                request.reminderType(),
                request.triggerDateTime(),
                request.recurring(),
                recurrenceType,
                request.enabled() == null ? reminder.isEnabled() : request.enabled(),
                request.sourceType(),
                request.sourceResourceId(),
                request.advanceMinutes()
        );
        return notificationMapper.toReminderResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public ReminderResponse updateEnabled(UUID userId, UUID id, UpdateReminderEnabledRequest request) {
        Reminder reminder = requireOwned(userId, id);
        if (Boolean.TRUE.equals(request.enabled())
                && reminder.isRecurring()
                && reminder.getRecurrenceType() == RecurrenceType.NONE) {
            throw new InvalidReminderStateException("Cannot enable a recurring reminder without DAILY or WEEKLY recurrence");
        }
        reminder.setEnabled(request.enabled());
        return notificationMapper.toReminderResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Reminder reminder = requireOwned(userId, id);
        reminder.softDelete();
        reminderRepository.save(reminder);
    }

    private Reminder requireOwned(UUID userId, UUID id) {
        return reminderRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(ReminderNotFoundException::new);
    }

    private void applyFields(
            Reminder reminder,
            String title,
            String message,
            ReminderType reminderType,
            Instant triggerDateTime,
            boolean recurring,
            RecurrenceType recurrenceType,
            boolean enabled,
            com.digitallifetwin.notification.enums.ReminderSourceType sourceType,
            UUID sourceResourceId,
            Integer advanceMinutes) {
        reminder.setTitle(title.trim());
        reminder.setMessage(trimToNull(message));
        reminder.setReminderType(reminderType);
        reminder.setTriggerDateTime(triggerDateTime);
        reminder.setRecurring(recurring);
        reminder.setRecurrenceType(recurrenceType);
        reminder.setEnabled(enabled);
        reminder.setSourceType(sourceType);
        reminder.setSourceResourceId(sourceResourceId);
        reminder.setAdvanceMinutes(advanceMinutes == null ? 0 : advanceMinutes);
        reminder.setNextTriggerAt(ReminderRecurrence.initialNextTrigger(triggerDateTime, reminder.getAdvanceMinutes()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
