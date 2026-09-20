package com.digitallifetwin.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "Google credential is required")
        String credential
) {
}
