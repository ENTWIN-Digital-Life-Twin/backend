package com.digitallifetwin.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.planning.dto.request.CreateEventRequest;
import com.digitallifetwin.planning.dto.response.EventResponse;
import com.digitallifetwin.planning.entity.CalendarEvent;
import com.digitallifetwin.planning.enums.EventType;
import com.digitallifetwin.planning.mapper.PlanningMapper;
import com.digitallifetwin.planning.repository.CalendarEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private CalendarEventRepository calendarEventRepository;
    @Mock
    private ScheduleConflictService scheduleConflictService;
    @Spy
    private PlanningMapper planningMapper = new PlanningMapper();

    @InjectMocks
    private EventService eventService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void createEvent_persistsNormalizedParticipants() {
        CreateEventRequest request = new CreateEventRequest(
                "Sprint review",
                "Weekly",
                Instant.parse("2026-08-23T10:00:00Z"),
                Instant.parse("2026-08-23T11:00:00Z"),
                false,
                EventType.WORK,
                "Room A",
                false,
                null,
                List.of(" Ada ", "Ada", "", "Karim", "   ")
        );
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
            CalendarEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(Instant.now());
            event.setUpdatedAt(Instant.now());
            return event;
        });
        when(scheduleConflictService.findConflictsForEvent(eq(userId), any(CalendarEvent.class), any()))
                .thenReturn(List.of());

        EventResponse response = eventService.create(userId, request);

        assertThat(response.participants()).containsExactly("Ada", "Karim");
        ArgumentCaptor<CalendarEvent> captor = ArgumentCaptor.forClass(CalendarEvent.class);
        verify(calendarEventRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipants()).containsExactly("Ada", "Karim");
    }
}
