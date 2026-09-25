package com.digitallifetwin.ai.security;

import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AiUserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String rawToken;
    private final Collection<? extends GrantedAuthority> authorities;

    public AiUserPrincipal(
            UUID id,
            String email,
            String rawToken,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.rawToken = rawToken;
        this.authorities = authorities;
    }

    public UUID getId() {
        return id;
    }

    public String getRawToken() {
        return rawToken;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
