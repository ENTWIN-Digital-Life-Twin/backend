package com.digitallifetwin.ai.security;

import com.digitallifetwin.ai.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID currentUserId() {
        return currentPrincipal().getId();
    }

    public static String currentRawToken() {
        return currentPrincipal().getRawToken();
    }

    private static AiUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AiUserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }
}
