package com.digitallifetwin.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user,
        boolean newDevice
) {
    public AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserResponse user) {
        this(accessToken, refreshToken, tokenType, expiresIn, user, false);
    }

    public AuthResponse withNewDevice(boolean flagged) {
        return new AuthResponse(accessToken, refreshToken, tokenType, expiresIn, user, flagged);
    }
}
