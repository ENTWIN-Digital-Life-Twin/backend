package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.dto.request.UpdateAccountStatusRequest;
import com.digitallifetwin.auth.entity.Role;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.mapper.UserMapper;
import com.digitallifetwin.auth.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ContactService contactService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Spy
    private UserMapper userMapper = new UserMapper();

    private AdminUserService adminUserService;
    private UUID adminId;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository, contactService, userMapper, refreshTokenService);
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
        Role role = new Role();
        role.setName(RoleName.USER);
        user = new User();
        user.setId(userId);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPreferredLanguage("en");
        user.setTimezone("UTC");
        user.setRoles(Set.of(role));
    }

    @Test
    void updateStatus_rejectsSelf() {
        assertThatThrownBy(() -> adminUserService.updateStatus(
                adminId, adminId, new UpdateAccountStatusRequest(AccountStatus.DISABLED)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatus_disablesAndRevokesRefreshTokens() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        var response = adminUserService.updateStatus(
                adminId, userId, new UpdateAccountStatusRequest(AccountStatus.DISABLED));

        assertThat(response.accountStatus()).isEqualTo(AccountStatus.DISABLED);
        verify(refreshTokenService).revokeAllForUser(user);
    }

    @Test
    void stats_aggregatesCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByAccountStatus(AccountStatus.ACTIVE)).thenReturn(8L);
        when(userRepository.countByRoleName(RoleName.ADMIN)).thenReturn(1L);
        when(contactService.count()).thenReturn(3L);

        var stats = adminUserService.stats();

        assertThat(stats.totalUsers()).isEqualTo(10);
        assertThat(stats.activeUsers()).isEqualTo(8);
        assertThat(stats.adminUsers()).isEqualTo(1);
        assertThat(stats.contactMessages()).isEqualTo(3);
    }
}
