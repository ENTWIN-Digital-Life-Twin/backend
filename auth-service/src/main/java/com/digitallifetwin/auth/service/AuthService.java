package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.dto.request.LoginRequest;
import com.digitallifetwin.auth.dto.request.LogoutRequest;
import com.digitallifetwin.auth.dto.request.RefreshTokenRequest;
import com.digitallifetwin.auth.dto.request.RegisterRequest;
import com.digitallifetwin.auth.dto.response.AuthResponse;
import com.digitallifetwin.auth.dto.response.UserResponse;
import java.util.UUID;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request, UUID currentUserId);
}
