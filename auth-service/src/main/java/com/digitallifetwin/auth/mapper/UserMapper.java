package com.digitallifetwin.auth.mapper;

import com.digitallifetwin.auth.dto.response.UserProfileResponse;
import com.digitallifetwin.auth.dto.response.UserResponse;
import com.digitallifetwin.auth.entity.Role;
import com.digitallifetwin.auth.entity.User;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roleNames(user)
        );
    }

    public UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getDateOfBirth(),
                user.getGender(),
                user.getHeightCm(),
                user.getWeightKg(),
                user.getOccupationType(),
                user.getPreferredLanguage(),
                user.getTimezone(),
                user.getAccountStatus(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt(),
                roleNames(user)
        );
    }

    private List<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .sorted()
                .toList();
    }
}
