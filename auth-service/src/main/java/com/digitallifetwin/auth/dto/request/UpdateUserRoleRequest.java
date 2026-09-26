package com.digitallifetwin.auth.dto.request;

import com.digitallifetwin.auth.enums.RoleName;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        RoleName role
) {
}
