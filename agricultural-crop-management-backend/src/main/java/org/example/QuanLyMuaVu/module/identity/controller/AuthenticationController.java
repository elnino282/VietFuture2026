package org.example.QuanLyMuaVu.module.identity.controller;

import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.text.ParseException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.identity.dto.request.AuthenticationRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.IntrospectRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.LogoutRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.RefreshRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.SignUpRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.AuthenticationResponse;
import org.example.QuanLyMuaVu.module.identity.dto.response.FarmerResponse;
import org.example.QuanLyMuaVu.module.identity.dto.response.IntrospectResponse;
import org.example.QuanLyMuaVu.module.identity.service.AuthenticationService;
import org.example.QuanLyMuaVu.module.identity.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller
 * 
 * Handles all authentication endpoints:
 * - POST /api/v1/auth/sign-in - Login with username/email + password
 * - GET /api/v1/auth/me - Get current user info (for frontend bootstrapping)
 * - POST /api/v1/auth/sign-up - Register new user
 * - POST /api/v1/auth/introspect - Validate token
 * - POST /api/v1/auth/refresh - Refresh token
 * - POST /api/v1/auth/sign-out - Logout (invalidate token)
 * - POST /api/v1/auth/forgot-password - Request password reset link
 * - POST /api/v1/auth/reset-password - Reset password via token
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthenticationController {

        AuthenticationService authenticationService;
        UserService userService;

        /**
         * Sign in with username/email and password.
         * 
         * Accepts identifier as either username or email.
         * Returns JWT token, basic profile, and redirect path based on role.
         * 
         * Error codes:
         * - 401 INVALID_CREDENTIALS: Wrong username/email or password
         * - 403 USER_LOCKED: User account is not active
         * - 403 ROLE_MISSING: User has no assigned role
         */
        @PostMapping("/sign-in")
        @Operation(summary = "Sign in user", description = "Authenticate user by username OR email + password. Returns JWT token, profile, and role-based redirect path.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "Success Response", value = """
                                        {
                                          "result": {
                                            "token": "eyJhbGciOiJIUzUxMiJ9...",
                                            "tokenType": "Bearer",
                                            "expiresIn": 3600,
                                            "role": "FARMER",
                                            "profile": {
                                              "id": 2,
                                              "fullName": "Nguyen Van T",
                                              "phone": "0900000001",
                                              "provinceId": 24,
                                              "wardId": 25112
                                            },
                                            "redirectTo": "/farmer"
                                          }
                                        }
                                        """))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                        {
                                          "code": "INVALID_CREDENTIALS",
                                          "message": "Invalid username/email or password."
                                        }
                                        """))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User locked or no role", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                        {
                                          "code": "USER_LOCKED",
                                          "message": "User account is not active."
                                        }
                                        """)))
        })
        ApiResponse<AuthenticationResponse> authenticate(
                        @Parameter(description = "Login credentials", required = true, content = @Content(examples = @ExampleObject(name = "Login Request", value = """
                                        {
                                          "identifier": "farmer1@acm.local",
                                          "password": "12345678",
                                          "rememberMe": false
                                        }
                                        """))) @Valid @RequestBody AuthenticationRequest request) {
                var result = authenticationService.authenticate(request);
                return ApiResponse.success(result);
        }

        /**
         * Get current user information.
         * 
         * Returns user profile, role, and redirect path for frontend bootstrapping.
         * Requires valid JWT token in Authorization header.
         */
        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get current user info", description = "Returns current user profile, role, and redirect path. Used for frontend app bootstrapping.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User info retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "Success Response", value = """
                                        {
                                          "result": {
                                            "userId": 2,
                                            "email": "farmer1@acm.local",
                                            "username": "farmer0",
                                            "roles": ["FARMER"],
                                            "role": "FARMER",
                                            "profile": {
                                              "id": 2,
                                              "fullName": "Nguyen Van T",
                                              "phone": "0900000001",
                                              "provinceId": 24,
                                              "wardId": 25112
                                            },
                                            "redirectTo": "/farmer"
                                          }
                                        }
                                        """))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or expired token")
        })
        ApiResponse<AuthenticationResponse> getCurrentUser() {
                var result = authenticationService.getCurrentUser();
                return ApiResponse.success(result);
        }

        @PostMapping("/sign-up")
        @Operation(summary = "Register new user", description = "Create new user account with BUYER or FARMER role")
        ApiResponse<FarmerResponse> signUp(
                        @RequestBody @Valid SignUpRequest request) {
                var result = userService.signUp(request);
                return ApiResponse.success(result);
        }

        @PostMapping("/introspect")
        @Operation(summary = "Validate token", description = "Check if JWT token is still valid")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token validation result", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
        })
        ApiResponse<IntrospectResponse> introspect(
                        @Parameter(description = "Token to validate", required = true) @RequestBody IntrospectRequest request)
                        throws ParseException, JOSEException {
                var result = authenticationService.introspect(request);
                return ApiResponse.success(result);
        }

        @PostMapping("/refresh")
        @Operation(summary = "Refresh JWT token", description = "Generate new token when current token is about to expire")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid or expired")
        })
        ApiResponse<AuthenticationResponse> refreshToken(
                        @Parameter(description = "Token to refresh", required = true) @RequestBody RefreshRequest request)
                        throws ParseException, JOSEException {
                var result = authenticationService.refreshToken(request);
                return ApiResponse.success(result);
        }

        @PreAuthorize("hasAnyRole('ADMIN','BUYER','FARMER','EMPLOYEE')")
        @PostMapping("/sign-out")
        @Operation(summary = "Sign out user", description = "Invalidate current JWT token")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid")
        })
        ApiResponse<Void> logout(
                        @Parameter(description = "Token to invalidate", required = true) @RequestBody LogoutRequest request)
                        throws ParseException, JOSEException {
                authenticationService.logout(request);
                return ApiResponse.success(null);
        }

}
