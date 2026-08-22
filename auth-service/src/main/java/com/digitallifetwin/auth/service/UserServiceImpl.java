package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.dto.request.ChangePasswordRequest;
import com.digitallifetwin.auth.dto.request.UpdateProfileRequest;
import com.digitallifetwin.auth.dto.response.MessageResponse;
import com.digitallifetwin.auth.dto.response.UserProfileResponse;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.exception.InvalidCredentialsException;
import com.digitallifetwin.auth.exception.UserNotFoundException;
import com.digitallifetwin.auth.mapper.UserMapper;
import com.digitallifetwin.auth.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(UUID currentUserId) {
        return userMapper.toProfileResponse(requireUser(currentUserId));
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentUser(UUID currentUserId, UpdateProfileRequest request) {
        User user = requireUser(currentUserId);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
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
        if (request.preferredLanguage() != null) {
            user.setPreferredLanguage(request.preferredLanguage().trim());
        }
        if (request.timezone() != null) {
            user.setTimezone(request.timezone().trim());
        }

        return userMapper.toProfileResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public MessageResponse changePassword(UUID currentUserId, ChangePasswordRequest request) {
        User user = requireUser(currentUserId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);
        return new MessageResponse("Password updated successfully");
    }

    private User requireUser(UUID userId) {
        return userRepository.findByIdWithRoles(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}
