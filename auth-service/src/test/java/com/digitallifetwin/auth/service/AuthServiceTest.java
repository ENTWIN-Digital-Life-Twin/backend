package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.dto.request.GoogleLoginRequest;
import com.digitallifetwin.auth.dto.request.LoginRequest;
import com.digitallifetwin.auth.dto.request.RegisterRequest;
import com.digitallifetwin.auth.dto.response.AuthResponse;
import com.digitallifetwin.auth.dto.response.UserResponse;
import com.digitallifetwin.auth.entity.RefreshToken;
import com.digitallifetwin.auth.entity.Role;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.entity.UserIdentity;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.Gender;
import com.digitallifetwin.auth.enums.IdentityProvider;
import com.digitallifetwin.auth.enums.OccupationType;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.exception.AccountDisabledException;
import com.digitallifetwin.auth.exception.AccountLinkingRequiredException;
import com.digitallifetwin.auth.exception.EmailAlreadyExistsException;
import com.digitallifetwin.auth.exception.GoogleEmailNotVerifiedException;
import com.digitallifetwin.auth.exception.GoogleLoginNotConfiguredException;
import com.digitallifetwin.auth.exception.InvalidCredentialsException;
import com.digitallifetwin.auth.exception.InvalidGoogleTokenException;
import com.digitallifetwin.auth.config.GoogleProperties;
import com.digitallifetwin.auth.google.GoogleIdTokenVerifierPort;
import com.digitallifetwin.auth.google.GoogleIdentity;
import com.digitallifetwin.auth.mapper.UserMapper;
import com.digitallifetwin.auth.repository.RoleRepository;
import com.digitallifetwin.auth.repository.UserIdentityRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import com.digitallifetwin.auth.security.JwtService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserIdentityRepository userIdentityRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private GoogleIdTokenVerifierPort googleIdTokenVerifier;
    @Mock
    private GoogleProperties googleProperties;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private AuthServiceImpl authService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(UUID.randomUUID());
        userRole.setName(RoleName.USER);
    }

    @Test
    void register_success_createsUserWithHashedPasswordAndDefaultRole() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "John@Example.com", "StrongPassword123!");
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        UserResponse response = authService.register(request);

        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.roles()).containsExactly("USER");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getPreference()).isNotNull();
        assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.isEmailVerified()).isTrue();
        verify(emailVerificationService).consume("john@example.com", "123456");
        verify(passwordEncoder).encode("StrongPassword123!");
    }

    @Test
    void register_withProfile_persistsBodyMetricsAndOccupation() {
        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "john@example.com",
                "StrongPassword123!",
                java.time.LocalDate.of(1998, 4, 12),
                Gender.FEMALE,
                168.0,
                62.5,
                OccupationType.STUDENT,
                "123456"
        );
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(saved.getHeightCm()).isEqualTo(168.0);
        assertThat(saved.getWeightKg()).isEqualTo(62.5);
        assertThat(saved.getOccupationType()).isEqualTo(OccupationType.STUDENT);
        assertThat(saved.getDateOfBirth()).isEqualTo(java.time.LocalDate.of(1998, 4, 12));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("John", "Doe", "john@example.com", "StrongPassword123!")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(emailVerificationService, never()).consume(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCaseWithRoles("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPassword123!", "hashed-password")).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessExpirationSeconds()).thenReturn(3600L);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        when(refreshTokenService.issue(user)).thenReturn(refreshToken);

        AuthResponse response = authService.login(new LoginRequest("john@example.com", "StrongPassword123!"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmailIgnoreCaseWithRoles("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "password12")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByEmailIgnoreCaseWithRoles("john@example.com")).thenReturn(Optional.of(activeUser()));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("john@example.com", "WrongPassword1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_disabledAccount_throwsAccountDisabled() {
        User user = activeUser();
        user.setAccountStatus(AccountStatus.DISABLED);
        when(userRepository.findByEmailIgnoreCaseWithRoles("john@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("john@example.com", "StrongPassword123!")))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void login_googleOnlyUserWithoutPassword_throwsInvalidCredentials() {
        User user = activeUser();
        user.setPasswordHash(null);
        when(userRepository.findByEmailIgnoreCaseWithRoles("john@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("john@example.com", "any-password")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwordEncoder, never()).matches(anyString(), any());
    }

    @Test
    void loginWithGoogle_invalidToken_throwsUnauthorized() {
        when(googleProperties.configured()).thenReturn(true);
        when(googleIdTokenVerifier.verify("bad-token")).thenThrow(new InvalidGoogleTokenException());

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleLoginRequest("bad-token")))
                .isInstanceOf(InvalidGoogleTokenException.class);
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    void loginWithGoogle_expiredToken_throwsUnauthorized() {
        when(googleProperties.configured()).thenReturn(true);
        when(googleIdTokenVerifier.verify("expired-token")).thenThrow(new InvalidGoogleTokenException());

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleLoginRequest("expired-token")))
                .isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void loginWithGoogle_wrongAudience_throwsUnauthorized() {
        when(googleProperties.configured()).thenReturn(true);
        when(googleIdTokenVerifier.verify("wrong-aud-token")).thenThrow(new InvalidGoogleTokenException());

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleLoginRequest("wrong-aud-token")))
                .isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void loginWithGoogle_notConfigured_throwsServiceUnavailable() {
        when(googleProperties.configured()).thenReturn(false);

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleLoginRequest("any-token")))
                .isInstanceOf(GoogleLoginNotConfiguredException.class);
        verify(googleIdTokenVerifier, never()).verify(anyString());
    }

    @Test
    void loginWithGoogle_unverifiedEmail_rejects() {
        when(googleProperties.configured()).thenReturn(true);
        when(googleIdTokenVerifier.verify("token")).thenReturn(googleIdentity(
                "sub-unverified", "user@gmail.com", false, null));
        when(userIdentityRepository.findByProviderAndProviderUserId(IdentityProvider.GOOGLE, "sub-unverified"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleLoginRequest("token")))
                .isInstanceOf(GoogleEmailNotVerifiedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithGoogle_newUser_createsUserWithDefaultRoleAndNoPassword() {
        GoogleIdentity identity = googleIdentity("sub-new", "new.user@gmail.com", true, null);
        stubGoogleCredential(identity);
        when(userIdentityRepository.findByProviderAndProviderUserId(IdentityProvider.GOOGLE, "sub-new"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseWithRoles("new.user@gmail.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        stubIssuedTokens();

        AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("valid-google-token"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().roles()).containsExactly("USER");
        assertThat(response.user().email()).isEqualTo("new.user@gmail.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        User created = userCaptor.getAllValues().get(0);
        assertThat(created.getPasswordHash()).isNull();
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(created.getRoles()).extracting(Role::getName).containsExactly(RoleName.USER);

        ArgumentCaptor<UserIdentity> identityCaptor = ArgumentCaptor.forClass(UserIdentity.class);
        verify(userIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo(IdentityProvider.GOOGLE);
        assertThat(identityCaptor.getValue().getProviderUserId()).isEqualTo("sub-new");
        verify(refreshTokenService).issue(any(User.class));
    }

    @Test
    void loginWithGoogle_returningUser_issuesEntwinTokensWithoutCreatingDuplicate() {
        User existing = activeUser();
        existing.setEmail("returning@gmail.com");
        existing.setPasswordHash(null);
        existing.setEmailVerified(true);
        UserIdentity linked = new UserIdentity();
        linked.setUser(existing);
        linked.setProvider(IdentityProvider.GOOGLE);
        linked.setProviderUserId("sub-returning");

        GoogleIdentity identity = googleIdentity("sub-returning", "returning@gmail.com", true, null);
        stubGoogleCredential(identity);
        when(userIdentityRepository.findByProviderAndProviderUserId(IdentityProvider.GOOGLE, "sub-returning"))
                .thenReturn(Optional.of(linked));
        stubIssuedTokens();

        AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("valid-google-token"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("returning@gmail.com");
        verify(userIdentityRepository, never()).save(any());
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    void loginWithGoogle_existingNonGoogleEmail_requiresExplicitLinking() {
        User existing = activeUser();
        existing.setEmail("ada@company.com");
        GoogleIdentity identity = googleIdentity("sub-work", "ada@company.com", true, null);
        stubGoogleCredential(identity);
        when(userIdentityRepository.findByProviderAndProviderUserId(IdentityProvider.GOOGLE, "sub-work"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseWithRoles("ada@company.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleLoginRequest("valid-google-token")))
                .isInstanceOf(AccountLinkingRequiredException.class);
        verify(userIdentityRepository, never()).save(any());
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    void loginWithGoogle_verifiedGmailMatchingLocalAccount_linksAndIssuesTokens() {
        User existing = activeUser();
        existing.setEmail("john@gmail.com");
        existing.setEmailVerified(false);
        GoogleIdentity identity = googleIdentity("sub-gmail", "john@gmail.com", true, null);
        stubGoogleCredential(identity);
        when(userIdentityRepository.findByProviderAndProviderUserId(IdentityProvider.GOOGLE, "sub-gmail"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseWithRoles("john@gmail.com")).thenReturn(Optional.of(existing));
        stubIssuedTokens();

        AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("valid-google-token"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(existing.isEmailVerified()).isTrue();
        verify(userIdentityRepository).save(any(UserIdentity.class));
        verify(refreshTokenService).issue(existing);
    }

    @Test
    void loginWithGoogle_workspaceHostedDomainMatchingLocalAccount_links() {
        User existing = activeUser();
        existing.setEmail("ada@entwin.test");
        GoogleIdentity identity = googleIdentity("sub-hd", "ada@entwin.test", true, "entwin.test");
        stubGoogleCredential(identity);
        when(userIdentityRepository.findByProviderAndProviderUserId(IdentityProvider.GOOGLE, "sub-hd"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseWithRoles("ada@entwin.test")).thenReturn(Optional.of(existing));
        stubIssuedTokens();

        AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("valid-google-token"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(userIdentityRepository).save(any(UserIdentity.class));
    }

    private void stubGoogleCredential(GoogleIdentity identity) {
        when(googleProperties.configured()).thenReturn(true);
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(identity);
    }

    private void stubIssuedTokens() {
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.getAccessExpirationSeconds()).thenReturn(3600L);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        when(refreshTokenService.issue(any(User.class))).thenReturn(refreshToken);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
    }

    private static GoogleIdentity googleIdentity(String subject, String email, boolean verified, String hostedDomain) {
        return new GoogleIdentity(subject, email, verified, "Ada", "Lovelace", hostedDomain);
    }

    private User activeUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPasswordHash("hashed-password");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRoles(Set.of(userRole));
        return user;
    }
}
