package com.digitallifetwin.auth.security;

import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.enums.AccountStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final AccountStatus accountStatus;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(
            UUID id,
            String email,
            String passwordHash,
            AccountStatus accountStatus,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountStatus = accountStatus;
        this.authorities = authorities;
    }

    public static CustomUserDetails from(User user, Collection<? extends GrantedAuthority> authorities) {
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getAccountStatus(),
                authorities
        );
    }

    public UUID getId() {
        return id;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
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
        return accountStatus != AccountStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus == AccountStatus.ACTIVE;
    }
}
