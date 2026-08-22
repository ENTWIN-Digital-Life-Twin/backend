package com.digitallifetwin.auth.dto.response;

import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.Gender;
import com.digitallifetwin.auth.enums.OccupationType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        LocalDate dateOfBirth,
        Gender gender,
        Double heightCm,
        Double weightKg,
        OccupationType occupationType,
        String preferredLanguage,
        String timezone,
        AccountStatus accountStatus,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt,
        List<String> roles
) {
}
