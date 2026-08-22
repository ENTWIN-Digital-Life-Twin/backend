package com.digitallifetwin.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitallifetwin.auth.entity.Role;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.digitallifetwin.auth.enums.RoleName;
import com.digitallifetwin.auth.repository.RoleRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void register_success() throws Exception {
        String email = uniqueEmail("success");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("John", "Doe", email, "StrongPassword123!")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_duplicateEmail_returnsConflict() throws Exception {
        String email = uniqueEmail("duplicate");
        register(email, "StrongPassword123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("John", "Doe", email, "StrongPassword123!")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void register_invalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("John", "Doe", "not-an-email", "StrongPassword123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void register_invalidPassword_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("John", "Doe", uniqueEmail("short"), "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_success_returnsTokens() throws Exception {
        String email = uniqueEmail("login");
        register(email, "StrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "StrongPassword123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.roles[0]").value("USER"));
    }

    @Test
    void login_unknownEmail_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown@example.com", "StrongPassword123!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        String email = uniqueEmail("wrongpass");
        register(email, "StrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "WrongPassword123!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_disabledAccount_returnsForbidden() throws Exception {
        String email = uniqueEmail("disabled");
        register(email, "StrongPassword123!");
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setAccountStatus(AccountStatus.DISABLED);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "StrongPassword123!")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void getMe_withoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getMe_withValidJwt_returnsProfile() throws Exception {
        String email = uniqueEmail("me");
        String accessToken = loginAndGetAccessToken(email, "StrongPassword123!");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void getMe_disabledUserWithPreviouslyValidJwt_returnsForbidden() throws Exception {
        String email = uniqueEmail("jwtdisabled");
        String accessToken = loginAndGetAccessToken(email, "StrongPassword123!");
        setAccountStatus(email, AccountStatus.DISABLED);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Account is disabled"))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    void getMe_blockedUserWithPreviouslyValidJwt_returnsForbidden() throws Exception {
        String email = uniqueEmail("jwtblocked");
        String accessToken = loginAndGetAccessToken(email, "StrongPassword123!");
        setAccountStatus(email, AccountStatus.BLOCKED);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Account is blocked"));
    }

    @Test
    void getMe_usesCurrentDatabaseRolesNotStaleJwtRoles() throws Exception {
        String email = uniqueEmail("rolerefresh");
        String accessToken = loginAndGetAccessToken(email, "StrongPassword123!");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        User user = userRepository.findByEmailIgnoreCaseWithRoles(email).orElseThrow();
        Role admin = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        user.getRoles().add(admin);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(2))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.roles[1]").value("USER"));
    }

    @Test
    void cors_allowsConfiguredOriginAndRejectsArbitraryOrigins() throws Exception {
        MockHttpServletRequest probe = new MockHttpServletRequest("GET", "/api/users/me");
        CorsConfiguration cors = corsConfigurationSource.getCorsConfiguration(probe);
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:4200");
        assertThat(cors.getAllowedOrigins()).doesNotContain("*");
        assertThat(cors.getAllowedOriginPatterns()).isNullOrEmpty();

        mockMvc.perform(options("/api/users/me")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(options("/api/users/me")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void getMe_withInvalidJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanUpdateOwnProfile() throws Exception {
        String email = uniqueEmail("profile");
        String accessToken = loginAndGetAccessToken(email, "StrongPassword123!");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updated",
                                  "occupationType": "STUDENT",
                                  "timezone": "Africa/Tunis"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.occupationType").value("STUDENT"))
                .andExpect(jsonPath("$.timezone").value("Africa/Tunis"))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void refreshAndLogout_rotateThenRevoke() throws Exception {
        String email = uniqueEmail("refresh");
        JsonNode login = login(email, "StrongPassword123!");
        String refreshToken = login.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String newRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        String accessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("accessToken").asText();
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefreshToken + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private void setAccountStatus(String email, AccountStatus status) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setAccountStatus(status);
        userRepository.saveAndFlush(user);
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("John", "Doe", email, password)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        return login(email, password).get("accessToken").asText();
    }

    private JsonNode login(String email, String password) throws Exception {
        register(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String registerJson(String firstName, String lastName, String email, String password) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(firstName, lastName, email, password);
    }

    private String loginJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}
