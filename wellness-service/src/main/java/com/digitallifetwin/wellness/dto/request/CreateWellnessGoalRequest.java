package com.digitallifetwin.wellness.dto.request;

import com.digitallifetwin.wellness.enums.WellnessGoalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateWellnessGoalRequest(
        @NotNull WellnessGoalType goalType,
        @NotNull @Positive Double targetValue,
        @DecimalMin("0") Double currentValue,
        @NotBlank @Size(max = 50) String unit,
        @NotNull LocalDate startDate,
        LocalDate targetDate
) {
}
