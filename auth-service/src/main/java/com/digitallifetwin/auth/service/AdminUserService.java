package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.dto.request.UpdateAccountStatusRequest;
import com.digitallifetwin.auth.dto.request.UpdateUserRoleRequest;
import com.digitallifetwin.auth.dto.response.AdminStatsResponse;
import com.digitallifetwin.auth.dto.response.UserProfileResponse;
import com.digitallifetwin.auth.entity.Role;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.exception.UserNotFoundException;
import com.digitallifetwin.auth.mapper.UserMapper;
import com.digitallifetwin.auth.repository.RoleRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ContactService contactService;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public List<UserProfileResponse> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(userMapper::toProfileResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByAccountStatus(AccountStatus.ACTIVE),
                userRepository.countByRoleName(RoleName.ADMIN),
                contactService.count()
        );
    }

    @Transactional
    public UserProfileResponse updateStatus(UUID adminId, UUID userId, UpdateAccountStatusRequest request) {
        if (adminId.equals(userId)) {
            throw new AccessDeniedException("Cannot change your own account status");
        }
        User user = userRepository.findByIdWithRoles(userId).orElseThrow(UserNotFoundException::new);
        user.setAccountStatus(request.accountStatus());
        userRepository.save(user);
        if (request.accountStatus() != AccountStatus.ACTIVE) {
            refreshTokenService.revokeAllForUser(user);
        }
        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateRole(UUID adminId, UUID userId, UpdateUserRoleRequest request) {
        if (adminId.equals(userId)) {
            throw new AccessDeniedException("Cannot change your own role");
        }
        if (request.role() != RoleName.ADMIN && request.role() != RoleName.USER) {
            throw new IllegalArgumentException("Role must be ADMIN or USER");
        }
        User user = userRepository.findByIdWithRoles(userId).orElseThrow(UserNotFoundException::new);
        boolean currentlyAdmin = hasRole(user, RoleName.ADMIN);
        if (request.role() == RoleName.ADMIN) {
            if (!currentlyAdmin) {
                user.getRoles().add(requireRole(RoleName.ADMIN));
            }
        } else if (currentlyAdmin) {
            if (userRepository.countByRoleName(RoleName.ADMIN) <= 1) {
                throw new AccessDeniedException("Cannot remove the last administrator");
            }
            user.getRoles().removeIf(role -> role.getName() == RoleName.ADMIN);
            if (user.getRoles().isEmpty()) {
                user.getRoles().add(requireRole(RoleName.USER));
            }
        }
        userRepository.save(user);
        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public void delete(UUID adminId, UUID userId) {
        if (adminId.equals(userId)) {
            throw new AccessDeniedException("Cannot delete your own account");
        }
        User user = userRepository.findByIdWithRoles(userId).orElseThrow(UserNotFoundException::new);
        if (hasRole(user, RoleName.ADMIN) && userRepository.countByRoleName(RoleName.ADMIN) <= 1) {
            throw new AccessDeniedException("Cannot remove the last administrator");
        }
        refreshTokenService.revokeAllForUser(user);
        userRepository.delete(user);
    }

    private Role requireRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Required role is missing: " + name));
    }

    private static boolean hasRole(User user, RoleName name) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == name);
    }
}
