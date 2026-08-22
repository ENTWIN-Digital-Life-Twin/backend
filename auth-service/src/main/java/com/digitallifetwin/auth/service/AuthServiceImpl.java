package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.dto.request.LoginRequest;
import com.digitallifetwin.auth.dto.request.LogoutRequest;
import com.digitallifetwin.auth.dto.request.RefreshTokenRequest;
import com.digitallifetwin.auth.dto.request.RegisterRequest;
import com.digitallifetwin.auth.dto.response.AuthResponse;
import com.digitallifetwin.auth.dto.response.UserResponse;
import com.digitallifetwin.auth.entity.RefreshToken;
import com.digitallifetwin.auth.entity.Role;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.entity.UserPreference;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.exception.AccountDisabledException;
import com.digitallifetwin.auth.exception.EmailAlreadyExistsException;
import com.digitallifetwin.auth.exception.InvalidCredentialsException;
import com.digitallifetwin.auth.exception.ResourceNotFoundException;
import com.digitallifetwin.auth.mapper.UserMapper;
import com.digitallifetwin.auth.repository.RoleRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import com.digitallifetwin.auth.security.JwtService;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default USER role is not configured"));

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPreferredLanguage("en");
        user.setTimezone("UTC");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setRoles(Set.of(userRole));
        user.assignPreference(UserPreference.defaultsFor(user));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCaseWithRoles(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);

        ensureAccountActive(user);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken rotated = refreshTokenService.rotate(request.refreshToken());
        User user = rotated.getUser();
        ensureAccountActive(user);
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                rotated.getToken(),
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                userMapper.toUserResponse(user)
        );
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request, UUID currentUserId) {
        refreshTokenService.revoke(request.refreshToken(), currentUserId);
    }

    private AuthResponse issueTokens(User user) {
        RefreshToken refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                refreshToken.getToken(),
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                userMapper.toUserResponse(user)
        );
    }

    private void ensureAccountActive(User user) {
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw new AccountDisabledException("Account is disabled");
        }
        if (user.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new AccountDisabledException("Account is blocked");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
