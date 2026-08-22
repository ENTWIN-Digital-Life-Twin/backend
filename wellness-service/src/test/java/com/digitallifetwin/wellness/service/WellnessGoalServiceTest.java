package com.digitallifetwin.wellness.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.digitallifetwin.wellness.dto.request.CreateWellnessGoalRequest;
import com.digitallifetwin.wellness.dto.request.UpdateGoalStatusRequest;
import com.digitallifetwin.wellness.dto.response.WellnessGoalResponse;
import com.digitallifetwin.wellness.entity.WellnessGoal;
import com.digitallifetwin.wellness.enums.GoalStatus;
import com.digitallifetwin.wellness.enums.WellnessGoalType;
import com.digitallifetwin.wellness.exception.InvalidGoalTransitionException;
import com.digitallifetwin.wellness.mapper.WellnessMapper;
import com.digitallifetwin.wellness.repository.WellnessGoalRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WellnessGoalServiceTest {

    @Mock
    private WellnessGoalRepository wellnessGoalRepository;
    @Spy
    private WellnessMapper wellnessMapper = new WellnessMapper();
    @InjectMocks
    private WellnessGoalService wellnessGoalService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void updateStatus_completedSetsCompletedAt() {
        WellnessGoal goal = activeGoal();
        when(wellnessGoalRepository.findByIdAndUserIdAndDeletedFalse(goal.getId(), userId))
                .thenReturn(Optional.of(goal));
        when(wellnessGoalRepository.save(goal)).thenReturn(goal);

        WellnessGoalResponse response = wellnessGoalService.updateStatus(
                userId, goal.getId(), new UpdateGoalStatusRequest(GoalStatus.COMPLETED));

        assertThat(response.status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(goal.getCompletedAt()).isNotNull();
    }

    @Test
    void updateStatus_invalidTransition_throws() {
        WellnessGoal goal = activeGoal();
        goal.setStatus(GoalStatus.COMPLETED);
        goal.setCompletedAt(Instant.now());
        when(wellnessGoalRepository.findByIdAndUserIdAndDeletedFalse(goal.getId(), userId))
                .thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> wellnessGoalService.updateStatus(
                userId, goal.getId(), new UpdateGoalStatusRequest(GoalStatus.CANCELLED)))
                .isInstanceOf(InvalidGoalTransitionException.class);
    }

    @Test
    void create_targetDateBeforeStart_throws() {
        assertThatThrownBy(() -> wellnessGoalService.create(userId, new CreateWellnessGoalRequest(
                WellnessGoalType.DAILY_WATER,
                2000.0,
                0.0,
                "ml",
                LocalDate.of(2026, 8, 22),
                LocalDate.of(2026, 8, 20)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target date");
    }

    private WellnessGoal activeGoal() {
        WellnessGoal goal = new WellnessGoal();
        goal.setId(UUID.randomUUID());
        goal.setUserId(userId);
        goal.setGoalType(WellnessGoalType.DAILY_WATER);
        goal.setTargetValue(2000.0);
        goal.setCurrentValue(0.0);
        goal.setUnit("ml");
        goal.setStartDate(LocalDate.of(2026, 8, 1));
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setCreatedAt(Instant.now());
        goal.setUpdatedAt(Instant.now());
        goal.setDeleted(false);
        return goal;
    }
}
