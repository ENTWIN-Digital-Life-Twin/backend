package com.digitallifetwin.notification.security;

import com.digitallifetwin.notification.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof NotificationUserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal.getId();
    }
}
