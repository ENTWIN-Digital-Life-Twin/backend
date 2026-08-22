package com.digitallifetwin.wellness.dto.request;

import com.digitallifetwin.wellness.enums.MealType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateMealRequest(
        @NotNull MealType mealType,
        @NotBlank @Size(max = 1000) String description,
        @NotNull Instant mealTime,
        @DecimalMin("0") Double totalCalories,
        @DecimalMin("0") Double proteinGrams,
        @DecimalMin("0") Double carbohydrateGrams,
        @DecimalMin("0") Double fatGrams,
        @DecimalMin("0") Double fibreGrams,
        @Size(max = 2000) String notes
) {
}
