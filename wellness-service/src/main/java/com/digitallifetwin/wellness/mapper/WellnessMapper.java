package com.digitallifetwin.wellness.mapper;

import com.digitallifetwin.wellness.dto.response.HealthRecordResponse;
import com.digitallifetwin.wellness.dto.response.MealResponse;
import com.digitallifetwin.wellness.dto.response.MoodResponse;
import com.digitallifetwin.wellness.dto.response.SleepResponse;
import com.digitallifetwin.wellness.dto.response.WaterResponse;
import com.digitallifetwin.wellness.dto.response.WellnessGoalResponse;
import com.digitallifetwin.wellness.dto.response.WorkoutResponse;
import com.digitallifetwin.wellness.entity.HealthRecord;
import com.digitallifetwin.wellness.entity.Meal;
import com.digitallifetwin.wellness.entity.MoodRecord;
import com.digitallifetwin.wellness.entity.SleepRecord;
import com.digitallifetwin.wellness.entity.WaterRecord;
import com.digitallifetwin.wellness.entity.WellnessGoal;
import com.digitallifetwin.wellness.entity.Workout;
import org.springframework.stereotype.Component;

@Component
public class WellnessMapper {

    public SleepResponse toSleepResponse(SleepRecord record) {
        return new SleepResponse(
                record.getId(),
                record.getUserId(),
                record.getSleepStart(),
                record.getWakeTime(),
                record.getDurationMinutes(),
                record.getQualityScore(),
                record.getInterruptions(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    public WaterResponse toWaterResponse(WaterRecord record) {
        return new WaterResponse(
                record.getId(),
                record.getUserId(),
                record.getQuantityMl(),
                record.getConsumedAt(),
                record.getBeverageType(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    public MealResponse toMealResponse(Meal meal) {
        return new MealResponse(
                meal.getId(),
                meal.getUserId(),
                meal.getMealType(),
                meal.getDescription(),
                meal.getMealTime(),
                meal.getTotalCalories(),
                meal.getProteinGrams(),
                meal.getCarbohydrateGrams(),
                meal.getFatGrams(),
                meal.getFibreGrams(),
                meal.getNotes(),
                meal.getCreatedAt(),
                meal.getUpdatedAt()
        );
    }

    public WorkoutResponse toWorkoutResponse(Workout workout) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getUserId(),
                workout.getActivityType(),
                workout.getStartedAt(),
                workout.getDurationMinutes(),
                workout.getIntensity(),
                workout.getCaloriesBurned(),
                workout.getAverageHeartRate(),
                workout.getDistanceKm(),
                workout.isCompleted(),
                workout.getNotes(),
                workout.getCreatedAt(),
                workout.getUpdatedAt()
        );
    }

    public MoodResponse toMoodResponse(MoodRecord record) {
        return new MoodResponse(
                record.getId(),
                record.getUserId(),
                record.getRecordedAt(),
                record.getMoodLevel(),
                record.getStressLevel(),
                record.getFatigueLevel(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    public HealthRecordResponse toHealthRecordResponse(HealthRecord record) {
        return new HealthRecordResponse(
                record.getId(),
                record.getUserId(),
                record.getRecordedAt(),
                record.getWeightKg(),
                record.getRestingHeartRate(),
                record.getSystolicPressure(),
                record.getDiastolicPressure(),
                record.getTemperatureCelsius(),
                record.getStepCount(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    public WellnessGoalResponse toWellnessGoalResponse(WellnessGoal goal) {
        return new WellnessGoalResponse(
                goal.getId(),
                goal.getUserId(),
                goal.getGoalType(),
                goal.getTargetValue(),
                goal.getCurrentValue(),
                goal.getUnit(),
                goal.getStartDate(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getCompletedAt(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
