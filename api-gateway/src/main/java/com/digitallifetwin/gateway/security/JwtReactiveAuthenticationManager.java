package com.digitallifetwin.gateway.security;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = String.valueOf(authentication.getCredentials());
        return Mono.fromCallable(() -> {
                    UUID userId = jwtService.extractUserId(token);
                    List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(token).stream()
                            .map(role -> new SimpleGrantedAuthority(
                                    role.startsWith("ROLE_") ? role : "ROLE_" + role))
                            .toList();
                    return (Authentication) new UsernamePasswordAuthenticationToken(
                            userId, token, authorities);
                })
                .onErrorMap(ex -> new BadCredentialsException("Invalid or expired token", ex));
    }
}
