package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.dto.request.UpdateAccountStatusRequest;
import com.digitallifetwin.auth.dto.request.UpdateUserRoleRequest;
import com.digitallifetwin.auth.entity.Role;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.mapper.UserMapper;
import com.digitallifetwin.auth.repository.RoleRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import java.util.HashSet;
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
    private RoleRepository roleRepository;
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
        adminUserService = new AdminUserService(
                userRepository, roleRepository, contactService, userMapper, refreshTokenService);
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
        user.setRoles(new HashSet<>(Set.of(role)));
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

    @Test
    void updateRole_rejectsSelf() {
        assertThatThrownBy(() -> adminUserService.updateRole(
                adminId, adminId, new UpdateUserRoleRequest(RoleName.USER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateRole_promotesUserToAdmin() {
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(user)).thenReturn(user);

        var response = adminUserService.updateRole(adminId, userId, new UpdateUserRoleRequest(RoleName.ADMIN));

        assertThat(response.roles()).contains("ADMIN");
    }

    @Test
    void updateRole_rejectsRemovingLastAdmin() {
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        user.setRoles(new HashSet<>(Set.of(adminRole)));
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(userRepository.countByRoleName(RoleName.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.updateRole(
                adminId, userId, new UpdateUserRoleRequest(RoleName.USER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateRole_demotesAdminWhenOthersRemain() {
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        Role userRole = new Role();
        userRole.setName(RoleName.USER);
        user.setRoles(new HashSet<>(Set.of(adminRole, userRole)));
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(userRepository.countByRoleName(RoleName.ADMIN)).thenReturn(2L);
        when(userRepository.save(user)).thenReturn(user);

        var response = adminUserService.updateRole(adminId, userId, new UpdateUserRoleRequest(RoleName.USER));

        assertThat(response.roles()).containsExactly("USER");
    }

    @Test
    void delete_rejectsSelf() {
        assertThatThrownBy(() -> adminUserService.delete(adminId, adminId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void delete_revokesTokensAndRemovesUser() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));

        adminUserService.delete(adminId, userId);

        verify(refreshTokenService).revokeAllForUser(user);
        verify(userRepository).delete(user);
    }
}
