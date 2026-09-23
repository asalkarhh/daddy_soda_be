package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.auth.LoginRequestDto;
import com.asalkartechworks.daddysodahouse.dto.auth.LoginResponseDto;
import com.asalkartechworks.daddysodahouse.dto.auth.UserSummaryDto;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller providing login, current user profile, and logout endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication and session management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate using 10-digit mobile number and password to obtain a JWT access token")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto loginResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(loginResponse, "Login successful"));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description = "Retrieve the authenticated user's details excluding password",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<UserSummaryDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserSummaryDto userSummary = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success(userSummary, "Current user retrieved successfully"));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Stateless logout notification instructing the client to remove the JWT access token",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Void>> logout() {
        String message = authService.logout();
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
