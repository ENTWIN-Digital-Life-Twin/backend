package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.dto.request.GoogleLoginRequest;
import com.digitallifetwin.auth.dto.request.LoginRequest;
import com.digitallifetwin.auth.dto.request.LogoutRequest;
import com.digitallifetwin.auth.dto.request.RefreshTokenRequest;
import com.digitallifetwin.auth.dto.request.RegisterRequest;
import com.digitallifetwin.auth.dto.response.AuthResponse;
import com.digitallifetwin.auth.dto.response.UserResponse;
import com.digitallifetwin.auth.entity.RefreshToken;
import com.digitallifetwin.auth.entity.Role;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.entity.UserIdentity;
import com.digitallifetwin.auth.entity.UserPreference;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.IdentityProvider;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.exception.AccountDisabledException;
import com.digitallifetwin.auth.exception.AccountLinkingRequiredException;
import com.digitallifetwin.auth.exception.EmailAlreadyExistsException;
import com.digitallifetwin.auth.exception.GoogleLoginNotConfiguredException;
import com.digitallifetwin.auth.exception.GoogleEmailNotVerifiedException;
import com.digitallifetwin.auth.exception.InvalidCredentialsException;
import com.digitallifetwin.auth.exception.ResourceNotFoundException;
import com.digitallifetwin.auth.google.GoogleIdTokenVerifierPort;
import com.digitallifetwin.auth.google.GoogleIdentity;
import com.digitallifetwin.auth.mapper.UserMapper;
import com.digitallifetwin.auth.repository.RoleRepository;
import com.digitallifetwin.auth.repository.UserIdentityRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import com.digitallifetwin.auth.config.GoogleProperties;
import com.digitallifetwin.auth.security.JwtService;
import java.time.Instant;
import java.util.HashSet;
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
    private final UserIdentityRepository userIdentityRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final GoogleIdTokenVerifierPort googleIdTokenVerifier;
    private final GoogleProperties googleProperties;
    private final EmailVerificationService emailVerificationService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        emailVerificationService.consume(email, request.verificationCode());

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
        user.setEmailVerified(true);
        user.setRoles(new HashSet<>(Set.of(userRole)));
        user.assignPreference(UserPreference.defaultsFor(user));
        applyOptionalProfile(user, request);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCaseWithRoles(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);

        ensureAccountActive(user);

        String passwordHash = user.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()
                || !passwordEncoder.matches(request.password(), passwordHash)) {
            throw new InvalidCredentialsException();
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        if (!googleProperties.configured()) {
            throw new GoogleLoginNotConfiguredException();
        }
        GoogleIdentity googleIdentity = googleIdTokenVerifier.verify(request.credential());
        User user = userIdentityRepository
                .findByProviderAndProviderUserId(IdentityProvider.GOOGLE, googleIdentity.subject())
                .map(UserIdentity::getUser)
                .orElseGet(() -> resolveOrCreateGoogleUser(googleIdentity));
        ensureAccountActive(user);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return issueTokens(user);
    }

    private User resolveOrCreateGoogleUser(GoogleIdentity identity) {
        if (identity.email() == null || identity.email().isBlank() || !identity.emailVerified()) {
            throw new GoogleEmailNotVerifiedException();
        }
        String email = normalizeEmail(identity.email());
        return userRepository.findByEmailIgnoreCaseWithRoles(email)
                .map(existing -> linkIfSafe(existing, identity))
                .orElseGet(() -> createGoogleUser(identity, email));
    }

    private User linkIfSafe(User existing, GoogleIdentity identity) {
        if (!identity.googleAuthoritativeEmail()) {
            throw new AccountLinkingRequiredException();
        }
        attachGoogleIdentity(existing, identity);
        existing.setEmailVerified(true);
        return existing;
    }

    private User createGoogleUser(GoogleIdentity identity, String email) {
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default USER role is not configured"));

        User user = new User();
        user.setFirstName(clipName(firstName(identity)));
        user.setLastName(clipName(lastName(identity)));
        user.setEmail(email);
        user.setPasswordHash(null);
        user.setPreferredLanguage("en");
        user.setTimezone("UTC");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setRoles(new HashSet<>(Set.of(userRole)));
        user.assignPreference(UserPreference.defaultsFor(user));

        User saved = userRepository.save(user);
        attachGoogleIdentity(saved, identity);
        return saved;
    }

    private void attachGoogleIdentity(User user, GoogleIdentity identity) {
        UserIdentity row = new UserIdentity();
        row.setUser(user);
        row.setProvider(IdentityProvider.GOOGLE);
        row.setProviderUserId(identity.subject());
        userIdentityRepository.save(row);
    }

    private static String firstName(GoogleIdentity identity) {
        if (hasText(identity.givenName())) {
            return identity.givenName().trim();
        }
        if (hasText(identity.email()) && identity.email().contains("@")) {
            return identity.email().substring(0, identity.email().indexOf('@'));
        }
        return "Google";
    }

    private static String lastName(GoogleIdentity identity) {
        if (hasText(identity.familyName())) {
            return identity.familyName().trim();
        }
        return "User";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String clipName(String value) {
        if (value.length() <= 100) {
            return value;
        }
        return value.substring(0, 100);
    }

    private static void applyOptionalProfile(User user, RegisterRequest request) {
        if (request.dateOfBirth() != null) {
            user.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.heightCm() != null) {
            user.setHeightCm(request.heightCm());
        }
        if (request.weightKg() != null) {
            user.setWeightKg(request.weightKg());
        }
        if (request.occupationType() != null) {
            user.setOccupationType(request.occupationType());
        }
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
