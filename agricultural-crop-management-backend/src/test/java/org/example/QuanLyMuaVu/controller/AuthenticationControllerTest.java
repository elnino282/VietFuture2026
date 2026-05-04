package org.example.QuanLyMuaVu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.QuanLyMuaVu.module.identity.dto.request.AuthenticationRequest;
import org.example.QuanLyMuaVu.module.identity.entity.Role;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.module.identity.repository.RoleRepository;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthenticationController.
 * Tests the sign-in endpoint with various scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Sql(scripts = "/sql/auth-schema-compat.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String SIGN_IN_URL = "/api/v1/auth/sign-in";
    private static final String TEST_EMAIL = "test@acm.local";
    private static final String TEST_PASSWORD = "testpassword123";

    @BeforeEach
    void setUp() {
        // Create test role if not exists
        Role farmerRole = roleRepository.findById(2L).orElseGet(() -> {
            Role role = Role.builder()
                    .code("FARMER")
                    .name("Farmer")
                    .description("Test farmer role")
                    .build();
            return roleRepository.save(role);
        });

        // Create active test user with BCrypt hashed password
        User activeUser = User.builder()
                .email(TEST_EMAIL)
                .username("testuser")
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(farmerRole))
                .build();
        userRepository.save(activeUser);
    }

    @Test
    @DisplayName("Sign in with valid credentials returns 200 and JWT token")
    void signIn_WithValidCredentials_ReturnsToken() throws Exception {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .rememberMe(false)
                .build();

        mockMvc.perform(post(SIGN_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.token").exists())
                .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.result.expiresIn").isNumber())
                .andExpect(jsonPath("$.result.userId").isNumber())
                .andExpect(jsonPath("$.result.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.result.username").value("testuser"))
                .andExpect(jsonPath("$.result.roles[0]").value("FARMER"));
    }

    @Test
    @DisplayName("Sign in with wrong password returns 401 Unauthorized")
    void signIn_WithWrongPassword_Returns401() throws Exception {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email(TEST_EMAIL)
                .password("wrongpassword")
                .rememberMe(false)
                .build();

        mockMvc.perform(post(SIGN_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Sign in with non-existent email returns 401 Unauthorized")
    void signIn_WithNonExistentEmail_Returns401() throws Exception {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("nonexistent@acm.local")
                .password(TEST_PASSWORD)
                .rememberMe(false)
                .build();

        mockMvc.perform(post(SIGN_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Sign in with inactive user returns 403 Forbidden")
    void signIn_WithInactiveUser_Returns403() throws Exception {
        // Create inactive user
        Role farmerRole = roleRepository.findById(2L).orElseThrow();
        User inactiveUser = User.builder()
                .email("inactive@acm.local")
                .username("inactiveuser")
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .status(UserStatus.INACTIVE)
                .roles(Set.of(farmerRole))
                .build();
        userRepository.save(inactiveUser);

        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("inactive@acm.local")
                .password(TEST_PASSWORD)
                .rememberMe(false)
                .build();

        mockMvc.perform(post(SIGN_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR_USER_INACTIVE"));
    }

    @Test
    @DisplayName("Sign in with invalid email format returns 400 Bad Request")
    void signIn_WithInvalidEmailFormat_Returns400() throws Exception {
        String invalidRequest = """
                {
                    "email": "not-an-email",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post(SIGN_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sign in with case insensitive email works correctly")
    void signIn_WithCaseInsensitiveEmail_ReturnsToken() throws Exception {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("TEST@ACM.LOCAL") // Uppercase email
                .password(TEST_PASSWORD)
                .rememberMe(false)
                .build();

        mockMvc.perform(post(SIGN_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.token").exists());
    }
}
