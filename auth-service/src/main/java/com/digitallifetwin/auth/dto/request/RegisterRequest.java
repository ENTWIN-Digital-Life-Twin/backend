package com.digitallifetwin.auth.dto.request;

import com.digitallifetwin.auth.enums.Gender;
import com.digitallifetwin.auth.enums.OccupationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        Gender gender,

        @Positive(message = "Height must be positive")
        Double heightCm,

        @Positive(message = "Weight must be positive")
        Double weightKg,

        OccupationType occupationType,

        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits")
        String verificationCode
) {
    @JsonIgnore
    public RegisterRequest(String firstName, String lastName, String email, String password) {
        this(firstName, lastName, email, password, null, null, null, null, null, "123456");
    }
}
