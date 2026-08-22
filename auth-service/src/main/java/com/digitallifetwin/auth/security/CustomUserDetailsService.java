package com.digitallifetwin.auth.security;

import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.repository.UserRepository;
import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmailIgnoreCaseWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toUserDetails(user);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(UUID id) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toUserDetails(user);
    }

    private CustomUserDetails toUserDetails(User user) {
        return CustomUserDetails.from(user, authorities(user));
    }

    public Collection<GrantedAuthority> authorities(User user) {
        return user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();
    }
}
