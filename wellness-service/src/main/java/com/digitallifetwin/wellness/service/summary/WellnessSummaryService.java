package com.digitallifetwin.wellness.service.summary;

import com.digitallifetwin.wellness.config.WellnessProperties;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse.ActivitySummary;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse.HydrationSummary;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse.NutritionSummary;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse.SleepSummary;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse.WellbeingSummary;
import com.digitallifetwin.wellness.dto.response.WeeklyWellnessSummaryResponse;
import com.digitallifetwin.wellness.entity.HealthRecord;
import com.digitallifetwin.wellness.entity.Meal;
import com.digitallifetwin.wellness.entity.MoodRecord;
import com.digitallifetwin.wellness.entity.SleepRecord;
import com.digitallifetwin.wellness.entity.WaterRecord;
import com.digitallifetwin.wellness.entity.Workout;
import com.digitallifetwin.wellness.repository.HealthRecordRepository;
import com.digitallifetwin.wellness.repository.MealRepository;
import com.digitallifetwin.wellness.repository.MoodRecordRepository;
import com.digitallifetwin.wellness.repository.SleepRecordRepository;
import com.digitallifetwin.wellness.repository.WaterRecordRepository;
import com.digitallifetwin.wellness.repository.WorkoutRepository;
import com.digitallifetwin.wellness.service.HydrationContribution;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WellnessSummaryService {

    private final WellnessProperties wellnessProperties;
    private final SleepRecordRepository sleepRecordRepository;
    private final WaterRecordRepository waterRecordRepository;
    private final MealRepository mealRepository;
    private final WorkoutRepository workoutRepository;
    private final MoodRecordRepository moodRecordRepository;
    private final HealthRecordRepository healthRecordRepository;

    @Transactional(readOnly = true)
    public DailyWellnessSummaryResponse daily(UUID userId, LocalDate date) {
        ZoneId zone = wellnessProperties.zoneId();
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        return buildDaily(userId, date, zone, start, end);
    }

    @Transactional(readOnly = true)
    public WeeklyWellnessSummaryResponse weekly(UUID userId, LocalDate startDate) {
        ZoneId zone = wellnessProperties.zoneId();
        LocalDate endDate = startDate.plusDays(6);
        List<DailyWellnessSummaryResponse> days = new ArrayList<>(7);

        int sleepDays = 0;
        long sleepMinutesSum = 0;
        double qualitySum = 0;
        int qualityCount = 0;
        int hydrationTotal = 0;
        int workoutMinutes = 0;
        int workoutCount = 0;
        double moodSum = 0;
        double stressSum = 0;
        double fatigueSum = 0;
        int moodCount = 0;
        long stepsSum = 0;
        double caloriesSum = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate day = startDate.plusDays(i);
            Instant start = day.atStartOfDay(zone).toInstant();
            Instant end = day.plusDays(1).atStartOfDay(zone).toInstant();
            DailyWellnessSummaryResponse daily = buildDaily(userId, day, zone, start, end);
            days.add(daily);

            if (daily.sleep().totalMinutes() != null && daily.sleep().totalMinutes() > 0) {
                sleepDays++;
                sleepMinutesSum += daily.sleep().totalMinutes();
            }
            hydrationTotal += daily.hydration().totalMl();
            workoutMinutes += daily.activity().activeMinutes();
            workoutCount += daily.activity().workoutCount();
            if (daily.activity().steps() != null) {
                stepsSum += daily.activity().steps();
            }
            caloriesSum += daily.nutrition().totalCalories() != null ? daily.nutrition().totalCalories() : 0.0;
        }

        Instant weekStart = startDate.atStartOfDay(zone).toInstant();
        Instant weekEnd = endDate.plusDays(1).atStartOfDay(zone).toInstant();

        List<SleepRecord> weekSleep = sleepRecordRepository
                .findByUserIdAndDeletedFalseAndWakeTimeGreaterThanEqualAndWakeTimeLessThan(
                        userId, weekStart, weekEnd);
        for (SleepRecord sleep : weekSleep) {
            if (sleep.getQualityScore() != null) {
                qualitySum += sleep.getQualityScore();
                qualityCount++;
            }
        }

        List<MoodRecord> weekMoods = moodRecordRepository
                .findByUserIdAndDeletedFalseAndRecordedAtGreaterThanEqualAndRecordedAtLessThan(
                        userId, weekStart, weekEnd);
        for (MoodRecord mood : weekMoods) {
            moodSum += mood.getMoodLevel();
            stressSum += mood.getStressLevel();
            fatigueSum += mood.getFatigueLevel();
            moodCount++;
        }

        Double averageSleepMinutes = sleepDays == 0 ? null : (double) sleepMinutesSum / sleepDays;
        Double averageSleepQuality = qualityCount == 0 ? null : qualitySum / qualityCount;
        Double averageHydrationMl = hydrationTotal / 7.0;
        Double averageMood = moodCount == 0 ? null : moodSum / moodCount;
        Double averageStress = moodCount == 0 ? null : stressSum / moodCount;
        Double averageFatigue = moodCount == 0 ? null : fatigueSum / moodCount;
        Double averageDailySteps = stepsSum / 7.0;

        return new WeeklyWellnessSummaryResponse(
                startDate,
                endDate,
                zone.getId(),
                averageSleepMinutes,
                averageSleepQuality,
                averageHydrationMl,
                workoutMinutes,
                workoutCount,
                averageMood,
                averageStress,
                averageFatigue,
                averageDailySteps,
                caloriesSum,
                days
        );
    }

    private DailyWellnessSummaryResponse buildDaily(
            UUID userId, LocalDate date, ZoneId zone, Instant start, Instant end) {
        List<SleepRecord> sleeps = sleepRecordRepository
                .findByUserIdAndDeletedFalseAndWakeTimeGreaterThanEqualAndWakeTimeLessThan(
                        userId, start, end);
        List<WaterRecord> waters = waterRecordRepository
                .findByUserIdAndDeletedFalseAndConsumedAtGreaterThanEqualAndConsumedAtLessThan(
                        userId, start, end);
        List<Meal> meals = mealRepository
                .findByUserIdAndDeletedFalseAndMealTimeGreaterThanEqualAndMealTimeLessThan(
                        userId, start, end);
        List<Workout> workouts = workoutRepository
                .findByUserIdAndDeletedFalseAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                        userId, start, end);
        List<MoodRecord> moods = moodRecordRepository
                .findByUserIdAndDeletedFalseAndRecordedAtGreaterThanEqualAndRecordedAtLessThan(
                        userId, start, end);
        List<HealthRecord> healthRecords = healthRecordRepository
                .findByUserIdAndDeletedFalseAndRecordedAtGreaterThanEqualAndRecordedAtLessThan(
                        userId, start, end);

        return new DailyWellnessSummaryResponse(
                date,
                zone.getId(),
                buildSleep(sleeps),
                buildHydration(waters),
                buildNutrition(meals),
                buildActivity(workouts, healthRecords),
                buildWellbeing(moods)
        );
    }

    private SleepSummary buildSleep(List<SleepRecord> sleeps) {
        if (sleeps.isEmpty()) {
            return new SleepSummary(0, 0.0, null);
        }
        int totalMinutes = sleeps.stream()
                .map(SleepRecord::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        List<Integer> qualities = sleeps.stream()
                .map(SleepRecord::getQualityScore)
                .filter(Objects::nonNull)
                .toList();
        Double averageQuality = qualities.isEmpty()
                ? null
                : qualities.stream().mapToInt(Integer::intValue).average().orElse(0);
        return new SleepSummary(totalMinutes, totalMinutes / 60.0, averageQuality);
    }

    private HydrationSummary buildHydration(List<WaterRecord> waters) {
        int totalMl = waters.stream()
                .mapToInt(w -> HydrationContribution.contributeMl(w.getBeverageType(), w.getQuantityMl()))
                .sum();
        int goalMl = wellnessProperties.waterGoalMl();
        Double goalPercentage = goalMl == 0 ? 0.0 : totalMl * 100.0 / goalMl;
        return new HydrationSummary(totalMl, goalMl, goalPercentage);
    }

    private NutritionSummary buildNutrition(List<Meal> meals) {
        if (meals.isEmpty()) {
            return new NutritionSummary(0, 0.0, 0.0, 0.0, 0.0);
        }
        return new NutritionSummary(
                meals.size(),
                sumDoubles(meals.stream().map(Meal::getTotalCalories).toList()),
                sumDoubles(meals.stream().map(Meal::getProteinGrams).toList()),
                sumDoubles(meals.stream().map(Meal::getCarbohydrateGrams).toList()),
                sumDoubles(meals.stream().map(Meal::getFatGrams).toList())
        );
    }

    private ActivitySummary buildActivity(List<Workout> workouts, List<HealthRecord> healthRecords) {
        int workoutCount = workouts.size();
        int activeMinutes = workouts.stream()
                .map(Workout::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        Double caloriesBurned = workouts.stream()
                .map(Workout::getCaloriesBurned)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        if (workouts.isEmpty()) {
            caloriesBurned = 0.0;
        }

        List<Integer> stepValues = healthRecords.stream()
                .map(HealthRecord::getStepCount)
                .filter(Objects::nonNull)
                .toList();
        Integer steps = stepValues.isEmpty()
                ? null
                : stepValues.stream().mapToInt(Integer::intValue).sum();

        return new ActivitySummary(workoutCount, activeMinutes, caloriesBurned, steps);
    }

    private WellbeingSummary buildWellbeing(List<MoodRecord> moods) {
        if (moods.isEmpty()) {
            return new WellbeingSummary(null, null, null);
        }
        double moodAvg = moods.stream().mapToInt(MoodRecord::getMoodLevel).average().orElse(0);
        double stressAvg = moods.stream().mapToInt(MoodRecord::getStressLevel).average().orElse(0);
        double fatigueAvg = moods.stream().mapToInt(MoodRecord::getFatigueLevel).average().orElse(0);
        return new WellbeingSummary(moodAvg, stressAvg, fatigueAvg);
    }

    private static Double sumDoubles(List<Double> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}
