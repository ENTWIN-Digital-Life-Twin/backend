package com.digitallifetwin.auth.dto.request;

import com.digitallifetwin.auth.enums.Gender;
import com.digitallifetwin.auth.enums.OccupationType;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
        String firstName,

        @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
        String lastName,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        Gender gender,

        @Positive(message = "Height must be positive")
        Double heightCm,

        @Positive(message = "Weight must be positive")
        Double weightKg,

        OccupationType occupationType,

        @Size(min = 2, max = 20, message = "Preferred language must be between 2 and 20 characters")
        String preferredLanguage,

        @Size(min = 1, max = 100, message = "Timezone must be between 1 and 100 characters")
        String timezone
) {
}
