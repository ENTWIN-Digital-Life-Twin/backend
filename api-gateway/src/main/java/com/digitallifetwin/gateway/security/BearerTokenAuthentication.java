package com.digitallifetwin.gateway.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Unauthenticated token holder passed to {@link JwtReactiveAuthenticationManager}.
 */
public final class BearerTokenAuthentication implements Authentication {

    private final String token;

    public BearerTokenAuthentication(String token) {
        this.token = token;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Cannot set this token to authenticated");
        }
    }

    @Override
    public String getName() {
        return "bearer";
    }
}
