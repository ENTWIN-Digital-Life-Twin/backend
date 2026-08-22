package com.digitallifetwin.wellness.dto.response;

import com.digitallifetwin.wellness.enums.MealType;
import java.time.Instant;
import java.util.UUID;

public record MealResponse(
        UUID id,
        UUID userId,
        MealType mealType,
        String description,
        Instant mealTime,
        Double totalCalories,
        Double proteinGrams,
        Double carbohydrateGrams,
        Double fatGrams,
        Double fibreGrams,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
