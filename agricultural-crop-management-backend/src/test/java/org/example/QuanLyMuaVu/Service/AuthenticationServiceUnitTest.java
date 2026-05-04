package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.example.QuanLyMuaVu.module.identity.dto.request.AuthenticationRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.IntrospectRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.LogoutRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.RefreshRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.AuthenticationResponse;
import org.example.QuanLyMuaVu.module.identity.dto.response.IntrospectResponse;
import org.example.QuanLyMuaVu.module.identity.entity.Role;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.identity.repository.InvalidatedTokenRepository;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.example.QuanLyMuaVu.module.identity.service.AuthenticationService;
import org.example.QuanLyMuaVu.module.identity.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AuthenticationService.
 * 
 * Tests cover:
 * - AUTHENTICATE: Valid credentials, invalid password, user not found, inactive
 * user,
 * locked user, no roles assigned, identifier variations (email/username)
 * - INTROSPECT: Valid token, invalid token
 * - LOGOUT: Successful logout, expired token
 * - REFRESH TOKEN: Valid refresh, invalid token
 * - HELPER METHODS: Role priority (ADMIN > FARMER > BUYER), redirect path
 * determination
 * 
 * Follows AAA (Arrange-Act-Assert) pattern.
 */
@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    // Test fixtures
    private User activeUserWithFarmerRole;
    private User activeUserWithAdminRole;
    private User activeUserWithBuyerRole;
    private User activeUserWithMultipleRoles;
    private User inactiveUser;
    private User lockedUser;
    private User userWithNoRoles;
    private Role farmerRole;
    private Role buyerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Arrange: Create test roles
        farmerRole = Role.builder()
                .id(2L)
                .code("FARMER")
                .name("Farmer")
                .build();

        buyerRole = Role.builder()
                .id(3L)
                .code("BUYER")
                .name("Buyer")
                .build();

        adminRole = Role.builder()
                .id(1L)
                .code("ADMIN")
                .name("Administrator")
                .build();

        // Arrange: Create test users
        activeUserWithFarmerRole = User.builder()
                .id(1L)
                .email("farmer@test.com")
                .username("farmer_user")
                .password("encodedPassword")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(farmerRole))
                .build();

        activeUserWithAdminRole = User.builder()
                .id(2L)
                .email("admin@test.com")
                .username("admin_user")
                .password("encodedPassword")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build();

        activeUserWithBuyerRole = User.builder()
                .id(3L)
                .email("buyer@test.com")
                .username("buyer_user")
                .password("encodedPassword")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(buyerRole))
                .build();

        activeUserWithMultipleRoles = User.builder()
                .id(4L)
                .email("multi@test.com")
                .username("multi_user")
                .password("encodedPassword")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(farmerRole, buyerRole))
                .build();

        inactiveUser = User.builder()
                .id(5L)
                .email("inactive@test.com")
                .username("inactive_user")
                .password("encodedPassword")
                .status(UserStatus.INACTIVE)
                .roles(Set.of(farmerRole))
                .build();

        lockedUser = User.builder()
                .id(6L)
                .email("locked@test.com")
                .username("locked_user")
                .password("encodedPassword")
                .status(UserStatus.LOCKED)
                .roles(Set.of(farmerRole))
                .build();

        userWithNoRoles = User.builder()
                .id(7L)
                .email("noroles@test.com")
                .username("noroles_user")
                .password("encodedPassword")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build();
    }

    // =========================================================================
    // AUTHENTICATE TESTS
    // =========================================================================

    @Nested
    @DisplayName("authenticate() Tests")
    class AuthenticateTests {

        @Test
        @DisplayName("Happy Path: Authenticates user with valid email and password")
        void authenticate_WithValidEmailAndPassword_ReturnsAuthResponse() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("farmer@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("farmer@test.com"))
                    .thenReturn(Optional.of(activeUserWithFarmerRole));
            when(passwordEncoder.matches("correctPassword", "encodedPassword"))
                    .thenReturn(true);
            when(jwtTokenService.generateToken(activeUserWithFarmerRole, "FARMER"))
                    .thenReturn("test.jwt.token");
            when(jwtTokenService.getValidDuration()).thenReturn(3600L);

            // Act
            AuthenticationResponse response = authenticationService.authenticate(request);

            // Assert
            assertNotNull(response, "Response should not be null");
            assertEquals("test.jwt.token", response.getToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(1L, response.getUserId());
            assertEquals("farmer@test.com", response.getEmail());
            assertEquals("farmer_user", response.getUsername());
            assertEquals("FARMER", response.getRole());
            assertEquals("/farmer", response.getRedirectTo());
            assertTrue(response.getRoles().contains("FARMER"));

            verify(userRepository).findByIdentifierWithRoles("farmer@test.com");
            verify(passwordEncoder).matches("correctPassword", "encodedPassword");
            verify(jwtTokenService).generateToken(activeUserWithFarmerRole, "FARMER");
        }

        @Test
        @DisplayName("Happy Path: Authenticates admin user and returns admin redirect")
        void authenticate_WithAdminUser_ReturnsAdminRedirect() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("admin@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("admin@test.com"))
                    .thenReturn(Optional.of(activeUserWithAdminRole));
            when(passwordEncoder.matches("correctPassword", "encodedPassword"))
                    .thenReturn(true);
            when(jwtTokenService.generateToken(activeUserWithAdminRole, "ADMIN"))
                    .thenReturn("admin.jwt.token");
            when(jwtTokenService.getValidDuration()).thenReturn(3600L);

            // Act
            AuthenticationResponse response = authenticationService.authenticate(request);

            // Assert
            assertEquals("ADMIN", response.getRole());
            assertEquals("/admin", response.getRedirectTo());
        }

        @Test
        @DisplayName("Happy Path: Uses identifier field when both identifier and email provided")
        void authenticate_WithBothIdentifierAndEmail_UsesIdentifier() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .identifier("farmer_user")
                    .email("different@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("farmer_user"))
                    .thenReturn(Optional.of(activeUserWithFarmerRole));
            when(passwordEncoder.matches("correctPassword", "encodedPassword"))
                    .thenReturn(true);
            when(jwtTokenService.generateToken(any(), anyString()))
                    .thenReturn("test.jwt.token");
            when(jwtTokenService.getValidDuration()).thenReturn(3600L);

            // Act
            authenticationService.authenticate(request);

            // Assert
            verify(userRepository).findByIdentifierWithRoles("farmer_user");
            verify(userRepository, never()).findByIdentifierWithRoles("different@test.com");
        }

        @Test
        @DisplayName("Negative Case: Throws IDENTIFIER_REQUIRED when no identifier provided")
        void authenticate_WithNoIdentifier_ThrowsIdentifierRequired() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .password("password")
                    .build();

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> authenticationService.authenticate(request),
                    "Should throw AppException when no identifier");

            assertEquals(ErrorCode.IDENTIFIER_REQUIRED, exception.getErrorCode());
            verify(userRepository, never()).findByIdentifierWithRoles(any());
        }

        @Test
        @DisplayName("Negative Case: Throws INVALID_CREDENTIALS when user not found")
        void authenticate_WhenUserNotFound_ThrowsInvalidCredentials() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("nonexistent@test.com")
                    .password("password")
                    .build();

            when(userRepository.findByIdentifierWithRoles("nonexistent@test.com"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> authenticationService.authenticate(request),
                    "Should throw AppException when user not found");

            assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws INVALID_CREDENTIALS when password is wrong")
        void authenticate_WithWrongPassword_ThrowsInvalidCredentials() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("farmer@test.com")
                    .password("wrongPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("farmer@test.com"))
                    .thenReturn(Optional.of(activeUserWithFarmerRole));
            when(passwordEncoder.matches("wrongPassword", "encodedPassword"))
                    .thenReturn(false);

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> authenticationService.authenticate(request),
                    "Should throw AppException when password wrong");

            assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
            verify(jwtTokenService, never()).generateToken(any(), any());
        }

        @Test
        @DisplayName("Negative Case: Throws USER_INACTIVE when user is inactive")
        void authenticate_WhenUserInactive_ThrowsUserInactive() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("inactive@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("inactive@test.com"))
                    .thenReturn(Optional.of(inactiveUser));
            when(passwordEncoder.matches("correctPassword", "encodedPassword"))
                    .thenReturn(true);

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> authenticationService.authenticate(request),
                    "Should throw AppException when user inactive");

            assertEquals(ErrorCode.USER_INACTIVE, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws USER_LOCKED when user is locked")
        void authenticate_WhenUserLocked_ThrowsUserLocked() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("locked@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("locked@test.com"))
                    .thenReturn(Optional.of(lockedUser));
            when(passwordEncoder.matches("correctPassword", "encodedPassword"))
                    .thenReturn(true);

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> authenticationService.authenticate(request),
                    "Should throw AppException when user locked");

            assertEquals(ErrorCode.USER_LOCKED, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws ROLE_MISSING when user has no roles")
        void authenticate_WhenUserHasNoRoles_ThrowsRoleMissing() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("noroles@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("noroles@test.com"))
                    .thenReturn(Optional.of(userWithNoRoles));
            when(passwordEncoder.matches("correctPassword", "encodedPassword"))
                    .thenReturn(true);

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> authenticationService.authenticate(request),
                    "Should throw AppException when user has no roles");

            assertEquals(ErrorCode.ROLE_MISSING, exception.getErrorCode());
        }

        @Test
        @DisplayName("Edge Case: FARMER role takes priority over BUYER when user has both")
        void authenticate_WithMultipleRoles_FarmerTakesPriority() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("multi@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("multi@test.com"))
                    .thenReturn(Optional.of(activeUserWithMultipleRoles));
            when(passwordEncoder.matches("correctPassword", "encodedPassword"))
                    .thenReturn(true);
            when(jwtTokenService.generateToken(activeUserWithMultipleRoles, "FARMER"))
                    .thenReturn("test.jwt.token");
            when(jwtTokenService.getValidDuration()).thenReturn(3600L);

            // Act
            AuthenticationResponse response = authenticationService.authenticate(request);

            // Assert - FARMER should be primary role
            assertEquals("FARMER", response.getRole());
            verify(jwtTokenService).generateToken(activeUserWithMultipleRoles, "FARMER");
        }
    }

    // =========================================================================
    // INTROSPECT TESTS
    // =========================================================================

    @Nested
    @DisplayName("introspect() Tests")
    class IntrospectTests {

        @Test
        @DisplayName("Happy Path: Returns valid=true for valid token")
        void introspect_WithValidToken_ReturnsValid() throws JOSEException, ParseException {
            // Arrange
            IntrospectRequest request = IntrospectRequest.builder()
                    .token("valid.jwt.token")
                    .build();

            when(jwtTokenService.verifyToken("valid.jwt.token", false))
                    .thenReturn(mock(SignedJWT.class));

            // Act
            IntrospectResponse response = authenticationService.introspect(request);

            // Assert
            assertTrue(response.isValid(), "Valid token should return valid=true");
        }

        @Test
        @DisplayName("Negative Case: Returns valid=false for invalid token")
        void introspect_WithInvalidToken_ReturnsInvalid() throws JOSEException, ParseException {
            // Arrange
            IntrospectRequest request = IntrospectRequest.builder()
                    .token("invalid.jwt.token")
                    .build();

            when(jwtTokenService.verifyToken("invalid.jwt.token", false))
                    .thenThrow(new AppException(ErrorCode.UNAUTHENTICATED));

            // Act
            IntrospectResponse response = authenticationService.introspect(request);

            // Assert
            assertFalse(response.isValid(), "Invalid token should return valid=false");
        }
    }

    // =========================================================================
    // LOGOUT TESTS
    // =========================================================================

    @Nested
    @DisplayName("logout() Tests")
    class LogoutTests {

        @Test
        @DisplayName("Happy Path: Invalidates token on logout")
        void logout_WithValidToken_InvalidatesToken() throws ParseException, JOSEException {
            // Arrange
            LogoutRequest request = LogoutRequest.builder()
                    .token("valid.jwt.token")
                    .build();

            SignedJWT mockSignedJWT = mock(SignedJWT.class);
            JWTClaimsSet mockClaimsSet = mock(JWTClaimsSet.class);

            when(jwtTokenService.verifyToken("valid.jwt.token", true))
                    .thenReturn(mockSignedJWT);
            when(mockSignedJWT.getJWTClaimsSet()).thenReturn(mockClaimsSet);
            when(mockClaimsSet.getJWTID()).thenReturn("unique-jwt-id");
            when(mockClaimsSet.getExpirationTime()).thenReturn(new Date(System.currentTimeMillis() + 3600000));

            // Act
            authenticationService.logout(request);

            // Assert
            verify(invalidatedTokenRepository).save(argThat(token -> "unique-jwt-id".equals(token.getId())));
        }

        @Test
        @DisplayName("Edge Case: Handles already expired token gracefully")
        void logout_WithExpiredToken_HandlesGracefully() throws ParseException, JOSEException {
            // Arrange
            LogoutRequest request = LogoutRequest.builder()
                    .token("expired.jwt.token")
                    .build();

            when(jwtTokenService.verifyToken("expired.jwt.token", true))
                    .thenThrow(new AppException(ErrorCode.UNAUTHENTICATED));

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> authenticationService.logout(request),
                    "Logout should handle expired token gracefully");
            verify(invalidatedTokenRepository, never()).save(any());
        }
    }

    // =========================================================================
    // REFRESH TOKEN TESTS
    // =========================================================================

    @Nested
    @DisplayName("refreshToken() Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Happy Path: Refreshes token and invalidates old one")
        void refreshToken_WithValidToken_ReturnsNewToken() throws ParseException, JOSEException {
            // Arrange
            RefreshRequest request = RefreshRequest.builder()
                    .token("old.jwt.token")
                    .build();

            SignedJWT mockSignedJWT = mock(SignedJWT.class);
            JWTClaimsSet mockClaimsSet = mock(JWTClaimsSet.class);

            when(jwtTokenService.verifyToken("old.jwt.token", true))
                    .thenReturn(mockSignedJWT);
            when(mockSignedJWT.getJWTClaimsSet()).thenReturn(mockClaimsSet);
            when(mockClaimsSet.getJWTID()).thenReturn("old-jwt-id");
            when(mockClaimsSet.getExpirationTime()).thenReturn(new Date(System.currentTimeMillis() + 3600000));
            when(mockClaimsSet.getClaim("email")).thenReturn("farmer@test.com");

            when(userRepository.findByIdentifierWithRoles("farmer@test.com"))
                    .thenReturn(Optional.of(activeUserWithFarmerRole));
            when(jwtTokenService.generateToken(activeUserWithFarmerRole, "FARMER"))
                    .thenReturn("new.jwt.token");
            when(jwtTokenService.getValidDuration()).thenReturn(3600L);

            // Act
            AuthenticationResponse response = authenticationService.refreshToken(request);

            // Assert
            assertNotNull(response);
            assertEquals("new.jwt.token", response.getToken());
            assertEquals("Bearer", response.getTokenType());

            // Verify old token was invalidated
            verify(invalidatedTokenRepository).save(argThat(token -> "old-jwt-id".equals(token.getId())));
        }

        @Test
        @DisplayName("Negative Case: Throws when user not found during refresh")
        void refreshToken_WhenUserNotFound_ThrowsUnauthenticated() throws ParseException, JOSEException {
            // Arrange
            RefreshRequest request = RefreshRequest.builder()
                    .token("valid.jwt.token")
                    .build();

            SignedJWT mockSignedJWT = mock(SignedJWT.class);
            JWTClaimsSet mockClaimsSet = mock(JWTClaimsSet.class);

            when(jwtTokenService.verifyToken("valid.jwt.token", true))
                    .thenReturn(mockSignedJWT);
            when(mockSignedJWT.getJWTClaimsSet()).thenReturn(mockClaimsSet);
            when(mockClaimsSet.getJWTID()).thenReturn("jwt-id");
            when(mockClaimsSet.getExpirationTime()).thenReturn(new Date());
            when(mockClaimsSet.getClaim("email")).thenReturn("deleted@test.com");

            when(userRepository.findByIdentifierWithRoles("deleted@test.com"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> authenticationService.refreshToken(request),
                    "Should throw when user not found");

            assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
        }
    }

    // =========================================================================
    // ROLE PRIORITY AND REDIRECT PATH TESTS
    // =========================================================================

    @Nested
    @DisplayName("Role Priority and Redirect Path Tests")
    class RolePriorityTests {

        @Test
        @DisplayName("ADMIN role takes priority over FARMER and BUYER")
        void authenticate_WithAdminAndFarmerRoles_AdminTakesPriority() {
            // Arrange
            User adminFarmerUser = User.builder()
                    .id(10L)
                    .email("admin.farmer@test.com")
                    .username("admin_farmer")
                    .password("encodedPassword")
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(adminRole, farmerRole))
                    .build();

            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("admin.farmer@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("admin.farmer@test.com"))
                    .thenReturn(Optional.of(adminFarmerUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenService.generateToken(any(), eq("ADMIN"))).thenReturn("token");
            when(jwtTokenService.getValidDuration()).thenReturn(3600L);

            // Act
            AuthenticationResponse response = authenticationService.authenticate(request);

            // Assert
            assertEquals("ADMIN", response.getRole());
            assertEquals("/admin", response.getRedirectTo());
        }

        @Test
        @DisplayName("BUYER role gets buyer redirect path")
        void authenticate_WithBuyerRole_ReturnsBuyerRedirect() {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("buyer@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("buyer@test.com"))
                    .thenReturn(Optional.of(activeUserWithBuyerRole));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenService.generateToken(any(), eq("BUYER"))).thenReturn("token");
            when(jwtTokenService.getValidDuration()).thenReturn(3600L);

            // Act
            AuthenticationResponse response = authenticationService.authenticate(request);

            // Assert
            assertEquals("BUYER", response.getRole());
            assertEquals("/marketplace", response.getRedirectTo());
        }
    }

    // =========================================================================
    // PROFILE INFO MAPPING TESTS
    // =========================================================================

    @Nested
    @DisplayName("Profile Info Mapping Tests")
    class ProfileInfoTests {

        @Test
        @DisplayName("Includes profile info in response")
        void authenticate_ReturnsProfileInfo() {
            // Arrange
            User userWithFullProfile = User.builder()
                    .id(100L)
                    .email("profile@test.com")
                    .username("profile_user")
                    .fullName("John Doe")
                    .phone("1234567890")
                    .password("encodedPassword")
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(farmerRole))
                    .build();

            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("profile@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("profile@test.com"))
                    .thenReturn(Optional.of(userWithFullProfile));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenService.generateToken(any(), anyString())).thenReturn("token");
            when(jwtTokenService.getValidDuration()).thenReturn(3600L);

            // Act
            AuthenticationResponse response = authenticationService.authenticate(request);

            // Assert
            assertNotNull(response.getProfile(), "Profile should not be null");
            assertEquals(100L, response.getProfile().getId());
            assertEquals("John Doe", response.getProfile().getFullName());
            assertEquals("profile@test.com", response.getProfile().getEmail());
            assertEquals("1234567890", response.getProfile().getPhone());
            assertEquals("ACTIVE", response.getProfile().getStatus());
        }

        @Test
        @DisplayName("Handles null optional profile fields gracefully")
        void authenticate_WithNullProfileFields_HandlesGracefully() {
            // Arrange
            User minimalUser = User.builder()
                    .id(101L)
                    .email("minimal@test.com")
                    .username("minimal_user")
                    .password("encodedPassword")
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(farmerRole))
                    // fullName, phone, province, ward are all null
                    .build();

            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("minimal@test.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByIdentifierWithRoles("minimal@test.com"))
                    .thenReturn(Optional.of(minimalUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenService.generateToken(any(), anyString())).thenReturn("token");
            when(jwtTokenService.getValidDuration()).thenReturn(3600L);

            // Act & Assert - should not throw NullPointerException
            assertDoesNotThrow(() -> authenticationService.authenticate(request),
                    "Should handle null profile fields gracefully");
        }
    }
}
