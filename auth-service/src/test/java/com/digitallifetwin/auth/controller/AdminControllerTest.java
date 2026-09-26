package com.digitallifetwin.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.dto.request.UpdateUserRoleRequest;
import com.digitallifetwin.auth.dto.response.UserProfileResponse;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.security.CustomUserDetails;
import com.digitallifetwin.auth.service.AdminUserService;
import com.digitallifetwin.auth.service.ContactService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminUserService adminUserService;
    @Mock
    private ContactService contactService;

    private AdminController controller;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        controller = new AdminController(adminUserService, contactService);
        adminId = UUID.randomUUID();
        CustomUserDetails details = new CustomUserDetails(
                adminId, "admin@example.com", "hash", AccountStatus.ACTIVE, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateRole_delegatesToService() {
        UUID userId = UUID.randomUUID();
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(RoleName.ADMIN);
        UserProfileResponse profile = profile(userId);
        when(adminUserService.updateRole(adminId, userId, request)).thenReturn(profile);

        ResponseEntity<UserProfileResponse> response = controller.updateRole(userId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(profile);
        verify(adminUserService).updateRole(adminId, userId, request);
    }

    @Test
    void deleteContact_returnsNoContent() {
        UUID contactId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteContact(contactId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(contactService).delete(contactId);
    }

    private static UserProfileResponse profile(UUID userId) {
        return new UserProfileResponse(
                userId,
                "Jane",
                "Doe",
                "jane@example.com",
                null,
                null,
                null,
                null,
                null,
                "en",
                "UTC",
                AccountStatus.ACTIVE,
                true,
                null,
                null,
                null,
                null,
                List.of("ADMIN"));
    }
}
