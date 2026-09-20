package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.dto.request.UpdateAccountStatusRequest;
import com.digitallifetwin.auth.dto.response.AdminStatsResponse;
import com.digitallifetwin.auth.dto.response.UserProfileResponse;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.exception.UserNotFoundException;
import com.digitallifetwin.auth.mapper.UserMapper;
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
}
