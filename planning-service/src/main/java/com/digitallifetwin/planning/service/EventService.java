package com.digitallifetwin.planning.service;

import com.digitallifetwin.planning.dto.request.CreateEventRequest;
import com.digitallifetwin.planning.dto.request.UpdateEventRequest;
import com.digitallifetwin.planning.dto.response.EventResponse;
import com.digitallifetwin.planning.dto.response.PageResponse;
import com.digitallifetwin.planning.dto.response.ScheduleConflictResponse;
import com.digitallifetwin.planning.entity.CalendarEvent;
import com.digitallifetwin.planning.exception.EventNotFoundException;
import com.digitallifetwin.planning.mapper.PlanningMapper;
import com.digitallifetwin.planning.repository.CalendarEventRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final CalendarEventRepository calendarEventRepository;
    private final ScheduleConflictService scheduleConflictService;
    private final PlanningMapper planningMapper;

    @Transactional
    public EventResponse create(UUID userId, CreateEventRequest request) {
        validate(request.startDateTime(), request.endDateTime(), request.recurring(), request.recurrenceRule());

        CalendarEvent event = new CalendarEvent();
        event.setUserId(userId);
        apply(event, request.title(), request.description(), request.startDateTime(), request.endDateTime(),
                request.allDay(), request.eventType(), request.locationLabel(),
                request.recurring(), request.recurrenceRule(), request.participants());
        event.setDeleted(false);

        CalendarEvent saved = calendarEventRepository.save(event);
        List<ScheduleConflictResponse> conflicts =
                scheduleConflictService.findConflictsForEvent(userId, saved, saved.getId());
        return planningMapper.toEventResponse(saved, conflicts);
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID userId, UUID eventId) {
        CalendarEvent event = requireOwned(userId, eventId);
        List<ScheduleConflictResponse> conflicts =
                scheduleConflictService.findConflictsForEvent(userId, event, event.getId());
        return planningMapper.toEventResponse(event, conflicts);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> list(UUID userId, Instant from, Instant to, Pageable pageable) {
        Page<CalendarEvent> page = calendarEventRepository.findAll(
                CalendarEventRepository.withFilters(userId, from, to),
                pageable);
        List<EventResponse> content = page.getContent().stream()
                .map(planningMapper::toEventResponse)
                .toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional
    public EventResponse update(UUID userId, UUID eventId, UpdateEventRequest request) {
        validate(request.startDateTime(), request.endDateTime(), request.recurring(), request.recurrenceRule());
        CalendarEvent event = requireOwned(userId, eventId);
        apply(event, request.title(), request.description(), request.startDateTime(), request.endDateTime(),
                request.allDay(), request.eventType(), request.locationLabel(),
                request.recurring(), request.recurrenceRule(), request.participants());

        CalendarEvent saved = calendarEventRepository.save(event);
        List<ScheduleConflictResponse> conflicts =
                scheduleConflictService.findConflictsForEvent(userId, saved, saved.getId());
        return planningMapper.toEventResponse(saved, conflicts);
    }

    @Transactional
    public void delete(UUID userId, UUID eventId) {
        CalendarEvent event = requireOwned(userId, eventId);
        event.softDelete();
        calendarEventRepository.save(event);
    }

    private CalendarEvent requireOwned(UUID userId, UUID eventId) {
        return calendarEventRepository.findByIdAndUserIdAndDeletedFalse(eventId, userId)
                .orElseThrow(EventNotFoundException::new);
    }

    private void apply(
            CalendarEvent event,
            String title,
            String description,
            Instant start,
            Instant end,
            boolean allDay,
            com.digitallifetwin.planning.enums.EventType eventType,
            String locationLabel,
            boolean recurring,
            String recurrenceRule,
            List<String> participants) {
        event.setTitle(title.trim());
        event.setDescription(trimToNull(description));
        event.setStartDateTime(start);
        event.setEndDateTime(end);
        event.setAllDay(allDay);
        event.setEventType(eventType);
        event.setLocationLabel(trimToNull(locationLabel));
        event.setRecurring(recurring);
        event.setRecurrenceRule(recurring ? trimToNull(recurrenceRule) : null);
        event.setParticipants(normalizeParticipants(participants));
    }

    private void validate(Instant start, Instant end, boolean recurring, String recurrenceRule) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End date-time must be after start date-time");
        }
        if (recurring && (recurrenceRule == null || recurrenceRule.isBlank())) {
            throw new IllegalArgumentException("Recurrence rule is required when recurring is true");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> normalizeParticipants(List<String> names) {
        if (names == null || names.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> normalized = new ArrayList<>();
        for (String name : names) {
            if (normalized.size() >= 30) {
                break;
            }
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            if (trimmed.length() > 120) {
                trimmed = trimmed.substring(0, 120);
            }
            if (!normalized.contains(trimmed)) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }
}
