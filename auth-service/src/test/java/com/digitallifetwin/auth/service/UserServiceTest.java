package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.dto.request.ChangePasswordRequest;
import com.digitallifetwin.auth.dto.request.UpdateProfileRequest;
import com.digitallifetwin.auth.dto.response.UserProfileResponse;
import com.digitallifetwin.auth.entity.Role;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.Gender;
import com.digitallifetwin.auth.enums.OccupationType;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.exception.InvalidCredentialsException;
import com.digitallifetwin.auth.mapper.UserMapper;
import com.digitallifetwin.auth.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        Role role = new Role();
        role.setName(RoleName.USER);
        user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPasswordHash("hashed");
        user.setPreferredLanguage("en");
        user.setTimezone("UTC");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRoles(Set.of(role));
    }

    @Test
    void getCurrentUser_returnsProfile() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));

        UserProfileResponse profile = userService.getCurrentUser(userId);

        assertThat(profile.id()).isEqualTo(userId);
        assertThat(profile.email()).isEqualTo("john@example.com");
        assertThat(profile.roles()).containsExactly("USER");
    }

    @Test
    void updateCurrentUser_updatesSafeFieldsOnly() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse updated = userService.updateCurrentUser(userId, new UpdateProfileRequest(
                "Jane",
                "Smith",
                LocalDate.of(1998, 4, 12),
                Gender.FEMALE,
                168.0,
                62.5,
                OccupationType.STUDENT,
                "fr",
                "Africa/Tunis",
                "Engineering student"
        ));

        assertThat(updated.firstName()).isEqualTo("Jane");
        assertThat(updated.lastName()).isEqualTo("Smith");
        assertThat(updated.occupationType()).isEqualTo(OccupationType.STUDENT);
        assertThat(updated.timezone()).isEqualTo("Africa/Tunis");
        assertThat(updated.bio()).isEqualTo("Engineering student");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void changePassword_rejectsWrongCurrentPassword() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(
                userId, new ChangePasswordRequest("wrong", "NewPassword123!")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void changePassword_updatesHashAndRevokesRefreshTokens() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);

        userService.changePassword(userId, new ChangePasswordRequest("old-password", "NewPassword123!"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenService).revokeAllForUser(user);
    }

    @Test
    void changePassword_googleOnlyUserWithoutPassword_throwsInvalidCredentials() {
        user.setPasswordHash(null);
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(
                userId, new ChangePasswordRequest("old-password", "NewPassword123!")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
