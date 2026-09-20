package com.digitallifetwin.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.enums.IdentityProvider;
import com.digitallifetwin.auth.exception.InvalidGoogleTokenException;
import com.digitallifetwin.auth.google.GoogleIdTokenVerifierPort;
import com.digitallifetwin.auth.google.GoogleIdentity;
import com.digitallifetwin.auth.repository.UserIdentityRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

@SpringBootTest(properties = "auth.expose-reset-token=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class GoogleAuthIntegrationTest {

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
    private UserIdentityRepository userIdentityRepository;

    @MockitoBean
    private GoogleIdTokenVerifierPort googleIdTokenVerifier;

    @Test
    void googleLogin_validNewUser_returnsEntwinTokensAndUserRole() throws Exception {
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(new GoogleIdentity(
                "sub-new-1", "new.google@gmail.com", true, "Ada", "Lovelace", null));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"valid-google-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("new.google@gmail.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("USER"));

        User created = userRepository.findByEmailIgnoreCase("new.google@gmail.com").orElseThrow();
        assertThat(created.getPasswordHash()).isNull();
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(userIdentityRepository.findByProviderAndProviderUserId(IdentityProvider.GOOGLE, "sub-new-1"))
                .isPresent();
    }

    @Test
    void googleLogin_returningUser_reusesAccount() throws Exception {
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(new GoogleIdentity(
                "sub-return-1", "return.google@gmail.com", true, "Ada", "Lovelace", null));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"first\"}"))
                .andExpect(status().isOk());

        MvcResult second = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"second\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        assertThat(userRepository.findAll().stream()
                .filter(user -> "return.google@gmail.com".equalsIgnoreCase(user.getEmail()))
                .count()).isEqualTo(1);
        assertThat(second.getResponse().getContentAsString()).contains("return.google@gmail.com");
    }

    @Test
    void googleLogin_invalidToken_returnsUnauthorized() throws Exception {
        when(googleIdTokenVerifier.verify("bad-token")).thenThrow(new InvalidGoogleTokenException());

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"bad-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void googleLogin_existingNonAuthoritativeEmail_returnsConflict() throws Exception {
        registerLocal("ada@company.com");

        when(googleIdTokenVerifier.verify("link-token")).thenReturn(new GoogleIdentity(
                "sub-work-1", "ada@company.com", true, "Ada", "Lovelace", null));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"link-token\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ada@company.com","password":"StrongPassword123!"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void googleLogin_verifiedGmailMatchingLocalAccount_links() throws Exception {
        registerLocal("link.user@gmail.com");

        when(googleIdTokenVerifier.verify("gmail-token")).thenReturn(new GoogleIdentity(
                "sub-gmail-1", "link.user@gmail.com", true, "Ada", "Lovelace", null));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"gmail-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("link.user@gmail.com"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        assertThat(userIdentityRepository.findByProviderAndProviderUserId(IdentityProvider.GOOGLE, "sub-gmail-1"))
                .isPresent();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"link.user@gmail.com","password":"StrongPassword123!"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void googleLogin_blankCredential_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private void registerLocal(String email) throws Exception {
        MvcResult codeResult = mockMvc.perform(post("/api/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String code = objectMapper.readTree(codeResult.getResponse().getContentAsString())
                .get("verificationCode")
                .asText();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ada",
                                  "lastName": "Lovelace",
                                  "email": "%s",
                                  "password": "StrongPassword123!",
                                  "verificationCode": "%s"
                                }
                                """.formatted(email, code)))
                .andExpect(status().isCreated());
    }
}
