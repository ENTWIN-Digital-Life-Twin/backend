package com.digitallifetwin.auth.security;

import com.digitallifetwin.auth.exception.InvalidCredentialsException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new InvalidCredentialsException("Authentication required");
        }
        return userDetails.getId();
    }
}
