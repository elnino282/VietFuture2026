package org.example.QuanLyMuaVu.controller;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Authentication Controller.
 * 
 * Tests the following scenarios as per Feature 0.1 specification:
 * - Valid login returns token + role + redirectTo
 * - Login with username or email
 * - Invalid credentials -> 401 INVALID_CREDENTIALS
 * - Locked user -> 403 USER_LOCKED
 * - User without roles -> 403 ROLE_MISSING
 * - /me endpoint returns current user info
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/sql/auth-schema-compat.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
public class AuthenticationControllerIntegrationTest {

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

        private User activeUser;
        private User lockedUser;
        private User userWithoutRoles;
        private Role farmerRole;
        private Role buyerRole;
        private static final String TEST_PASSWORD = "testPassword123";

        @BeforeEach
        void setUp() {
                // Get or create roles
                farmerRole = roleRepository.findByCode("FARMER")
                                .orElseGet(() -> roleRepository.save(Role.builder()
                                                .code("FARMER")
                                                .name("Farmer")
                                                .description("Farm owner")
                                                .build()));

                buyerRole = roleRepository.findByCode("BUYER")
                                .orElseGet(() -> roleRepository.save(Role.builder()
                                                .code("BUYER")
                                                .name("Buyer")
                                                .description("Buyer user")
                                                .build()));

                // Create test users
                activeUser = User.builder()
                                .username("test_active_user")
                                .email("active@test.local")
                                .password(passwordEncoder.encode(TEST_PASSWORD))
                                .fullName("Active Test User")
                                .phone("0900000001")
                                .status(UserStatus.ACTIVE)
                                .roles(new HashSet<>(Set.of(farmerRole)))
                                .build();
                activeUser = userRepository.saveAndFlush(activeUser);

                lockedUser = User.builder()
                                .username("test_locked_user")
                                .email("locked@test.local")
                                .password(passwordEncoder.encode(TEST_PASSWORD))
                                .fullName("Locked Test User")
                                .phone("0900000002")
                                .status(UserStatus.LOCKED)
                                .roles(new HashSet<>(Set.of(farmerRole)))
                                .build();
                lockedUser = userRepository.saveAndFlush(lockedUser);

                userWithoutRoles = User.builder()
                                .username("test_no_roles_user")
                                .email("noroles@test.local")
                                .password(passwordEncoder.encode(TEST_PASSWORD))
                                .fullName("No Roles Test User")
                                .phone("0900000003")
                                .status(UserStatus.ACTIVE)
                                .roles(new HashSet<>())
                                .build();
                userWithoutRoles = userRepository.saveAndFlush(userWithoutRoles);
        }

        @Test
        @DisplayName("Valid login with email returns token, role, profile, and redirectTo")
        void testValidLoginWithEmail() throws Exception {
                AuthenticationRequest request = AuthenticationRequest.builder()
                                .identifier(activeUser.getEmail())
                                .password(TEST_PASSWORD)
                                .build();

                mockMvc.perform(post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.result.token").isNotEmpty())
                                .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
                                .andExpect(jsonPath("$.result.expiresIn").isNumber())
                                .andExpect(jsonPath("$.result.role").value("FARMER"))
                                .andExpect(jsonPath("$.result.roles", hasItem("FARMER")))
                                .andExpect(jsonPath("$.result.profile.id").value(activeUser.getId()))
                                .andExpect(jsonPath("$.result.profile.fullName").value(activeUser.getFullName()))
                                .andExpect(jsonPath("$.result.redirectTo").value("/farmer"));
        }

        @Test
        @DisplayName("Valid login with username returns token")
        void testValidLoginWithUsername() throws Exception {
                AuthenticationRequest request = AuthenticationRequest.builder()
                                .identifier(activeUser.getUsername())
                                .password(TEST_PASSWORD)
                                .build();

                mockMvc.perform(post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.result.token").isNotEmpty())
                                .andExpect(jsonPath("$.result.role").value("FARMER"));
        }

        @Test
        @DisplayName("Invalid credentials returns 401 INVALID_CREDENTIALS")
        void testInvalidCredentials() throws Exception {
                AuthenticationRequest request = AuthenticationRequest.builder()
                                .identifier(activeUser.getEmail())
                                .password("wrongPassword")
                                .build();

                mockMvc.perform(post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Non-existent user returns 401 INVALID_CREDENTIALS")
        void testNonExistentUser() throws Exception {
                AuthenticationRequest request = AuthenticationRequest.builder()
                                .identifier("nonexistent@test.local")
                                .password(TEST_PASSWORD)
                                .build();

                mockMvc.perform(post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Locked user returns 403 USER_LOCKED")
        void testLockedUser() throws Exception {
                AuthenticationRequest request = AuthenticationRequest.builder()
                                .identifier(lockedUser.getEmail())
                                .password(TEST_PASSWORD)
                                .build();

                mockMvc.perform(post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("USER_LOCKED"));
        }

        @Test
        @DisplayName("User without roles returns 403 ROLE_MISSING")
        void testUserWithoutRoles() throws Exception {
                AuthenticationRequest request = AuthenticationRequest.builder()
                                .identifier(userWithoutRoles.getEmail())
                                .password(TEST_PASSWORD)
                                .build();

                mockMvc.perform(post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("ROLE_MISSING"));
        }

        @Test
        @DisplayName("GET /me returns current user info when authenticated")
        void testGetCurrentUser() throws Exception {
                // First, login to get token
                AuthenticationRequest loginRequest = AuthenticationRequest.builder()
                                .identifier(activeUser.getEmail())
                                .password(TEST_PASSWORD)
                                .build();

                MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                String responseBody = loginResult.getResponse().getContentAsString();
                String token = objectMapper.readTree(responseBody).path("result").path("token").asText();

                // Then, call /me with token
                mockMvc.perform(get("/api/v1/auth/me")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.result.userId").value(activeUser.getId()))
                                .andExpect(jsonPath("$.result.email").value(activeUser.getEmail()))
                                .andExpect(jsonPath("$.result.role").value("FARMER"))
                                .andExpect(jsonPath("$.result.profile.fullName").value(activeUser.getFullName()));
        }

        @Test
        @DisplayName("GET /me without token returns 401")
        void testGetCurrentUserWithoutToken() throws Exception {
                mockMvc.perform(get("/api/v1/auth/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Buyer user gets role=BUYER and redirectTo=/marketplace")
        void testBuyerUserRedirect() throws Exception {
                User buyerUser = User.builder()
                                .username("test_buyer_user")
                                .email("buyer@test.local")
                                .password(passwordEncoder.encode(TEST_PASSWORD))
                                .fullName("Buyer Test User")
                                .phone("0900000004")
                                .status(UserStatus.ACTIVE)
                                .roles(new HashSet<>(Set.of(buyerRole)))
                                .build();
                buyerUser = userRepository.saveAndFlush(buyerUser);

                AuthenticationRequest request = AuthenticationRequest.builder()
                                .identifier(buyerUser.getEmail())
                                .password(TEST_PASSWORD)
                                .build();

                mockMvc.perform(post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.result.role").value("BUYER"))
                                .andExpect(jsonPath("$.result.redirectTo").value("/marketplace"));
        }

        @Test
        @DisplayName("Sign-up with BUYER role persists BUYER instead of FARMER")
        void testBuyerSignupPersistsBuyerRole() throws Exception {
                Map<String, Object> request = Map.of(
                                "username", "signup_buyer_user",
                                "email", "signupbuyer@test.local",
                                "password", TEST_PASSWORD,
                                "role", "BUYER");

                mockMvc.perform(post("/api/v1/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.result.username").value("signup_buyer_user"));

                User createdUser = userRepository.findByIdentifierWithRoles("signupbuyer@test.local")
                                .orElseThrow();

                Set<String> createdRoles = createdUser.getRoles().stream().map(Role::getCode).collect(java.util.stream.Collectors.toSet());
                assertTrue(createdRoles.contains("BUYER"));
                assertFalse(createdRoles.contains("FARMER"));
        }

        @Test
        @DisplayName("Buyer cannot access marketplace farmer or admin APIs")
        void testBuyerCannotAccessFarmerOrAdminMarketplaceApis() throws Exception {
                User buyerUser = User.builder()
                                .username("market_buyer_user")
                                .email("marketbuyer@test.local")
                                .password(passwordEncoder.encode(TEST_PASSWORD))
                                .fullName("Market Buyer")
                                .phone("0900000010")
                                .status(UserStatus.ACTIVE)
                                .roles(new HashSet<>(Set.of(buyerRole)))
                                .build();
                buyerUser = userRepository.saveAndFlush(buyerUser);

                AuthenticationRequest request = AuthenticationRequest.builder()
                                .identifier(buyerUser.getEmail())
                                .password(TEST_PASSWORD)
                                .build();

                MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                                .path("result")
                                .path("token")
                                .asText();

                mockMvc.perform(get("/api/v1/marketplace/farmer/dashboard")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/v1/marketplace/admin/stats")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("User with multiple roles gets FARMER as primary (FARMER preferred)")
        void testMultipleRolesPreferFarmer() throws Exception {
                User multiRoleUser = User.builder()
                                .username("test_multi_role_user")
                                .email("multirole@test.local")
                                .password(passwordEncoder.encode(TEST_PASSWORD))
                                .fullName("Multi Role Test User")
                                .phone("0900000005")
                                .status(UserStatus.ACTIVE)
                                .roles(new HashSet<>(Set.of(buyerRole, farmerRole)))
                                .build();
                multiRoleUser = userRepository.saveAndFlush(multiRoleUser);

                AuthenticationRequest request = AuthenticationRequest.builder()
                                .identifier(multiRoleUser.getEmail())
                                .password(TEST_PASSWORD)
                                .build();

                mockMvc.perform(post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.result.role").value("FARMER"))
                                .andExpect(jsonPath("$.result.redirectTo").value("/farmer"))
                                .andExpect(jsonPath("$.result.roles", hasItems("BUYER", "FARMER")));
        }
}
