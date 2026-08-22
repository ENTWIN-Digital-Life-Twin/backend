package com.digitallifetwin.wellness.service.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.digitallifetwin.wellness.config.WellnessProperties;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse;
import com.digitallifetwin.wellness.dto.response.WeeklyWellnessSummaryResponse;
import com.digitallifetwin.wellness.entity.HealthRecord;
import com.digitallifetwin.wellness.entity.Meal;
import com.digitallifetwin.wellness.entity.MoodRecord;
import com.digitallifetwin.wellness.entity.SleepRecord;
import com.digitallifetwin.wellness.entity.WaterRecord;
import com.digitallifetwin.wellness.entity.Workout;
import com.digitallifetwin.wellness.enums.ActivityType;
import com.digitallifetwin.wellness.enums.BeverageType;
import com.digitallifetwin.wellness.enums.IntensityLevel;
import com.digitallifetwin.wellness.enums.MealType;
import com.digitallifetwin.wellness.repository.HealthRecordRepository;
import com.digitallifetwin.wellness.repository.MealRepository;
import com.digitallifetwin.wellness.repository.MoodRecordRepository;
import com.digitallifetwin.wellness.repository.SleepRecordRepository;
import com.digitallifetwin.wellness.repository.WaterRecordRepository;
import com.digitallifetwin.wellness.repository.WorkoutRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WellnessSummaryServiceTest {

    @Mock
    private WellnessProperties wellnessProperties;
    @Mock
    private SleepRecordRepository sleepRecordRepository;
    @Mock
    private WaterRecordRepository waterRecordRepository;
    @Mock
    private MealRepository mealRepository;
    @Mock
    private WorkoutRepository workoutRepository;
    @Mock
    private MoodRecordRepository moodRecordRepository;
    @Mock
    private HealthRecordRepository healthRecordRepository;

    @InjectMocks
    private WellnessSummaryService wellnessSummaryService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(wellnessProperties.zoneId()).thenReturn(java.time.ZoneId.of("Africa/Casablanca"));
        when(wellnessProperties.waterGoalMl()).thenReturn(2000);
    }

    @Test
    void daily_calculatesExactTotals() {
        LocalDate date = LocalDate.of(2026, 8, 22);

        SleepRecord sleep = new SleepRecord();
        sleep.setDurationMinutes(480);
        sleep.setQualityScore(8);

        WaterRecord water = new WaterRecord();
        water.setQuantityMl(250);
        water.setBeverageType(BeverageType.TEA);

        Meal meal = new Meal();
        meal.setMealType(MealType.LUNCH);
        meal.setTotalCalories(600.0);
        meal.setProteinGrams(30.0);
        meal.setCarbohydrateGrams(50.0);
        meal.setFatGrams(20.0);

        Workout workout = new Workout();
        workout.setActivityType(ActivityType.RUNNING);
        workout.setDurationMinutes(40);
        workout.setCaloriesBurned(350.0);
        workout.setIntensity(IntensityLevel.HIGH);

        MoodRecord mood = new MoodRecord();
        mood.setMoodLevel(7);
        mood.setStressLevel(4);
        mood.setFatigueLevel(5);

        HealthRecord health = new HealthRecord();
        health.setStepCount(8000);

        stubDayEntries(List.of(sleep), List.of(water), List.of(meal), List.of(workout), List.of(mood), List.of(health));

        DailyWellnessSummaryResponse summary = wellnessSummaryService.daily(userId, date);

        assertThat(summary.sleep().totalMinutes()).isEqualTo(480);
        assertThat(summary.sleep().hours()).isEqualTo(8.0);
        assertThat(summary.sleep().averageQuality()).isEqualTo(8.0);
        assertThat(summary.hydration().totalMl()).isEqualTo(200);
        assertThat(summary.hydration().goalMl()).isEqualTo(2000);
        assertThat(summary.hydration().goalPercentage()).isEqualTo(10.0);
        assertThat(summary.nutrition().mealCount()).isEqualTo(1);
        assertThat(summary.nutrition().totalCalories()).isEqualTo(600.0);
        assertThat(summary.activity().workoutCount()).isEqualTo(1);
        assertThat(summary.activity().activeMinutes()).isEqualTo(40);
        assertThat(summary.activity().caloriesBurned()).isEqualTo(350.0);
        assertThat(summary.activity().steps()).isEqualTo(8000);
        assertThat(summary.wellbeing().averageMood()).isEqualTo(7.0);
        assertThat(summary.wellbeing().averageStress()).isEqualTo(4.0);
        assertThat(summary.wellbeing().averageFatigue()).isEqualTo(5.0);
    }

    @Test
    void weekly_aggregatesSevenDays() {
        LocalDate start = LocalDate.of(2026, 8, 17);
        stubEmptyDays();

        WeeklyWellnessSummaryResponse weekly = wellnessSummaryService.weekly(userId, start);

        assertThat(weekly.startDate()).isEqualTo(start);
        assertThat(weekly.endDate()).isEqualTo(start.plusDays(6));
        assertThat(weekly.days()).hasSize(7);
        assertThat(weekly.averageHydrationMl()).isEqualTo(0.0);
        assertThat(weekly.averageSleepMinutes()).isNull();
        assertThat(weekly.totalCaloriesConsumed()).isEqualTo(0.0);
    }

    private void stubDayEntries(
            List<SleepRecord> sleeps,
            List<WaterRecord> waters,
            List<Meal> meals,
            List<Workout> workouts,
            List<MoodRecord> moods,
            List<HealthRecord> healthRecords) {
        when(sleepRecordRepository.findByUserIdAndDeletedFalseAndWakeTimeGreaterThanEqualAndWakeTimeLessThan(
                eq(userId), any(Instant.class), any(Instant.class))).thenReturn(sleeps);
        when(waterRecordRepository.findByUserIdAndDeletedFalseAndConsumedAtGreaterThanEqualAndConsumedAtLessThan(
                eq(userId), any(Instant.class), any(Instant.class))).thenReturn(waters);
        when(mealRepository.findByUserIdAndDeletedFalseAndMealTimeGreaterThanEqualAndMealTimeLessThan(
                eq(userId), any(Instant.class), any(Instant.class))).thenReturn(meals);
        when(workoutRepository.findByUserIdAndDeletedFalseAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                eq(userId), any(Instant.class), any(Instant.class))).thenReturn(workouts);
        when(moodRecordRepository.findByUserIdAndDeletedFalseAndRecordedAtGreaterThanEqualAndRecordedAtLessThan(
                eq(userId), any(Instant.class), any(Instant.class))).thenReturn(moods);
        when(healthRecordRepository.findByUserIdAndDeletedFalseAndRecordedAtGreaterThanEqualAndRecordedAtLessThan(
                eq(userId), any(Instant.class), any(Instant.class))).thenReturn(healthRecords);
    }

    private void stubEmptyDays() {
        stubDayEntries(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
