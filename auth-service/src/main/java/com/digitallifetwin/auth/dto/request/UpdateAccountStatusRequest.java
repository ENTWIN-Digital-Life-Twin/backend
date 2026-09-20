package com.digitallifetwin.auth.dto.request;

import com.digitallifetwin.auth.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "Account status is required")
        AccountStatus accountStatus
) {
}
