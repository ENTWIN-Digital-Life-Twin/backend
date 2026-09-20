package com.digitallifetwin.auth.google;

public record GoogleIdentity(
        String subject,
        String email,
        boolean emailVerified,
        String givenName,
        String familyName,
        String hostedDomain
) {
    public boolean googleAuthoritativeEmail() {
        if (email == null || email.isBlank() || !emailVerified) {
            return false;
        }
        String lower = email.toLowerCase();
        if (lower.endsWith("@gmail.com") || lower.endsWith("@googlemail.com")) {
            return true;
        }
        return hostedDomain != null && !hostedDomain.isBlank();
    }
}
