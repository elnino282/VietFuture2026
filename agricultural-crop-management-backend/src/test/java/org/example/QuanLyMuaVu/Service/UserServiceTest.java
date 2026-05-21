package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.Constant.PredefinedRole;
import org.example.QuanLyMuaVu.module.identity.dto.request.ChangePasswordRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.SignUpRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.FarmerResponse;
import org.example.QuanLyMuaVu.module.identity.entity.Role;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.identity.mapper.FarmerMapper;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.identity.repository.RoleRepository;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.example.QuanLyMuaVu.module.identity.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

/**
 * Functional tests for UserService.
 * 
 * Covers key operations: sign up, profile update, password change.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private FarmerMapper farmerMapper;

        @Mock
        private FarmQueryPort farmQueryPort;

        @Mock
        private PasswordEncoder passwordEncoder;

        @InjectMocks
        private UserService userService;

        private User testUser;
        private Role farmerRole;
        private Role buyerRole;
        private FarmerResponse farmerResponse;

        @BeforeEach
        void setUp() {
                farmerRole = Role.builder()
                                .id(1L)
                                .code(PredefinedRole.FARMER_ROLE)
                                .name("Farmer")
                                .build();

                buyerRole = Role.builder()
                                .id(2L)
                                .code(PredefinedRole.BUYER_ROLE)
                                .name("Buyer")
                                .build();

                testUser = User.builder()
                                .id(1L)
                                .username("testfarmer")
                                .email("farmer@test.com")
                                .password("encodedPassword")
                                .status(UserStatus.ACTIVE)
                                .roles(Set.of(farmerRole))
                                .build();

                farmerResponse = FarmerResponse.builder()
                                .id(1L)
                                .username("testfarmer")
                                .email("farmer@test.com")
                                .build();
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        private void authenticateAsUserId(long userId) {
                SecurityContextHolder.getContext()
                                .setAuthentication(new TestingAuthenticationToken(
                                                String.valueOf(userId),
                                                "N/A",
                                                "ROLE_BUYER"));
        }

        @Test
        @DisplayName("SignUp - Creates user with valid data and FARMER role")
        void signUp_WithValidData_CreatesUserWithFarmerRole() {
                // Arrange
                SignUpRequest request = SignUpRequest.builder()
                                .username("newfarmer")
                                .password("Password123!")
                                .email("new@test.com")
                                .fullName("New Farmer")
                                .role(PredefinedRole.FARMER_ROLE)
                                .build();

                when(farmerMapper.toUser(any())).thenReturn(new User());
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(roleRepository.findByCode(PredefinedRole.FARMER_ROLE)).thenReturn(Optional.of(farmerRole));
                when(userRepository.save(any())).thenAnswer(i -> {
                        User u = i.getArgument(0);
                        u.setId(1L);
                        return u;
                });
                when(farmerMapper.toFarmerResponse(any())).thenReturn(farmerResponse);

                // Act
                FarmerResponse response = userService.signUp(request);

                // Assert
                assertNotNull(response);
                verify(passwordEncoder).encode("Password123!");
                verify(roleRepository).findByCode(PredefinedRole.FARMER_ROLE);
                verify(userRepository).save(any());
        }

        @Test
        @DisplayName("SignUp - Throws USERNAME_BLANK when username is empty")
        void signUp_WithBlankUsername_ThrowsAppException() {
                // Arrange
                SignUpRequest request = SignUpRequest.builder()
                                .username("   ")
                                .password("Password123!")
                                .build();

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> userService.signUp(request));

                assertEquals(ErrorCode.USERNAME_BLANK, exception.getErrorCode());
                verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("SignUp - Throws USER_EXISTED when username already exists")
        void signUp_WhenUsernameExists_ThrowsAppException() {
                // Arrange
                SignUpRequest request = SignUpRequest.builder()
                                .username("existinguser")
                                .password("Password123!")
                                .role(PredefinedRole.FARMER_ROLE)
                                .build();

                when(farmerMapper.toUser(any())).thenReturn(new User());
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(roleRepository.findByCode(PredefinedRole.FARMER_ROLE)).thenReturn(Optional.of(farmerRole));
                when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("Duplicate"));

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> userService.signUp(request));

                assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
        }

        @Test
        @DisplayName("ChangePassword - Updates password when current password is valid")
        void changeMyPassword_WithValidCurrentPassword_UpdatesPassword() {
                authenticateAsUserId(1L);
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("OldPass123!")
                                .newPassword("NewPass123!")
                                .build();

                when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(testUser));
                when(passwordEncoder.matches("OldPass123!", "encodedPassword")).thenReturn(true);
                when(passwordEncoder.encode("NewPass123!")).thenReturn("encodedNewPassword");
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(farmerMapper.toFarmerResponse(any(User.class))).thenReturn(farmerResponse);

                FarmerResponse response = userService.changeMyPassword(request);

                assertNotNull(response);
                verify(passwordEncoder).matches("OldPass123!", "encodedPassword");
                verify(passwordEncoder).encode("NewPass123!");
                verify(userRepository).save(argThat(user -> "encodedNewPassword".equals(user.getPassword())));
        }

        @Test
        @DisplayName("ChangePassword - Throws CURRENT_PASSWORD_INCORRECT when current password does not match")
        void changeMyPassword_WithWrongCurrentPassword_ThrowsAppException() {
                authenticateAsUserId(1L);
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("WrongPass123!")
                                .newPassword("NewPass123!")
                                .build();

                when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(testUser));
                when(passwordEncoder.matches("WrongPass123!", "encodedPassword")).thenReturn(false);

                AppException exception = assertThrows(AppException.class,
                                () -> userService.changeMyPassword(request));

                assertEquals(ErrorCode.CURRENT_PASSWORD_INCORRECT, exception.getErrorCode());
                verify(passwordEncoder, never()).encode(anyString());
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("ChangePassword - Throws PASSWORD_INVALID when new password is too short")
        void changeMyPassword_WithShortNewPassword_ThrowsAppException() {
                authenticateAsUserId(1L);
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("OldPass123!")
                                .newPassword("short")
                                .build();

                when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(testUser));

                AppException exception = assertThrows(AppException.class,
                                () -> userService.changeMyPassword(request));

                assertEquals(ErrorCode.PASSWORD_INVALID, exception.getErrorCode());
                verify(passwordEncoder, never()).encode(anyString());
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("ChangePassword - Throws PASSWORD_NOT_SET when account has no local password")
        void changeMyPassword_WithoutLocalPassword_ThrowsAppException() {
                authenticateAsUserId(1L);
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("OldPass123!")
                                .newPassword("NewPass123!")
                                .build();

                User oauthOnlyUser = User.builder()
                                .id(1L)
                                .username("oauth_user")
                                .password(null)
                                .roles(Set.of(farmerRole))
                                .build();
                when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(oauthOnlyUser));

                AppException exception = assertThrows(AppException.class,
                                () -> userService.changeMyPassword(request));

                assertEquals(ErrorCode.PASSWORD_NOT_SET, exception.getErrorCode());
                verify(passwordEncoder, never()).matches(anyString(), anyString());
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("ChangePassword - Throws UNAUTHENTICATED when no authenticated user in context")
        void changeMyPassword_WithoutAuthentication_ThrowsAppException() {
                SecurityContextHolder.clearContext();
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("OldPass123!")
                                .newPassword("NewPass123!")
                                .build();

                AppException exception = assertThrows(AppException.class,
                                () -> userService.changeMyPassword(request));

                assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("SignUp - Sets email from username when username contains @")
        void signUp_WithEmailAsUsername_SetsEmailFromUsername() {
                // Arrange
                SignUpRequest request = SignUpRequest.builder()
                                .username("farmer@example.com")
                                .password("Password123!")
                                .role(PredefinedRole.FARMER_ROLE)
                                .build();

                User newUser = new User();
                when(farmerMapper.toUser(any())).thenReturn(newUser);
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(roleRepository.findByCode(PredefinedRole.FARMER_ROLE)).thenReturn(Optional.of(farmerRole));
                when(userRepository.save(any())).thenAnswer(i -> {
                        User u = i.getArgument(0);
                        u.setId(1L);
                        return u;
                });
                when(farmerMapper.toFarmerResponse(any())).thenReturn(farmerResponse);

                // Act
                userService.signUp(request);

                // Assert
                verify(userRepository).save(argThat(user -> "farmer@example.com".equals(user.getEmail())));
        }

        @Test
        @DisplayName("SignUp - Creates user with BUYER role when requested")
        void signUp_WithBuyerRole_CreatesBuyerUser() {
                SignUpRequest request = SignUpRequest.builder()
                                .username("newbuyer")
                                .password("Password123!")
                                .email("buyer@test.com")
                                .role(PredefinedRole.BUYER_ROLE)
                                .build();

                when(farmerMapper.toUser(any())).thenReturn(new User());
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(roleRepository.findByCode(PredefinedRole.BUYER_ROLE)).thenReturn(Optional.of(buyerRole));
                when(userRepository.save(any())).thenAnswer(i -> {
                        User u = i.getArgument(0);
                        u.setId(2L);
                        return u;
                });
                when(farmerMapper.toFarmerResponse(any())).thenReturn(farmerResponse);

                userService.signUp(request);

                verify(roleRepository).findByCode(PredefinedRole.BUYER_ROLE);
        }
}
