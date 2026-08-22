package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.dto.request.LoginRequest;
import com.digitallifetwin.auth.dto.request.RegisterRequest;
import com.digitallifetwin.auth.dto.response.AuthResponse;
import com.digitallifetwin.auth.dto.response.UserResponse;
import com.digitallifetwin.auth.entity.RefreshToken;
import com.digitallifetwin.auth.entity.Role;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.exception.AccountDisabledException;
import com.digitallifetwin.auth.exception.EmailAlreadyExistsException;
import com.digitallifetwin.auth.exception.InvalidCredentialsException;
import com.digitallifetwin.auth.mapper.UserMapper;
import com.digitallifetwin.auth.repository.RoleRepository;
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
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
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
        verify(passwordEncoder).encode("StrongPassword123!");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("John", "Doe", "john@example.com", "StrongPassword123!")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsTokensAndUpdatesLastLogin() {
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
