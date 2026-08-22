package com.digitallifetwin.wellness.dto.request;

import com.digitallifetwin.wellness.enums.BeverageType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateWaterRequest(
        @NotNull @Min(1) Integer quantityMl,
        @NotNull Instant consumedAt,
        @NotNull BeverageType beverageType,
        @Size(max = 2000) String notes
) {
}
