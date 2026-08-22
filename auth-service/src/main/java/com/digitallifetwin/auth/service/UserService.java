package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.dto.request.ChangePasswordRequest;
import com.digitallifetwin.auth.dto.request.UpdateProfileRequest;
import com.digitallifetwin.auth.dto.response.MessageResponse;
import com.digitallifetwin.auth.dto.response.UserProfileResponse;
import java.util.UUID;

public interface UserService {

    UserProfileResponse getCurrentUser(UUID currentUserId);

    UserProfileResponse updateCurrentUser(UUID currentUserId, UpdateProfileRequest request);

    MessageResponse changePassword(UUID currentUserId, ChangePasswordRequest request);
}
