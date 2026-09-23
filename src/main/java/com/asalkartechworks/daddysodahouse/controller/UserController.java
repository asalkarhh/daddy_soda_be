package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.auth.UserSummaryDto;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.common.StatusUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.user.UserCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.user.UserUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "User Management", description = "User creation and franchise assignment (SUPER_ADMIN only)")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserManagementService userManagementService;

    @PostMapping
    @Operation(summary = "Create user", description = "Create MANAGER or FRANCHISE_OWNER with role validation and franchise assignment")
    public ResponseEntity<ApiResponse<UserSummaryDto>> createUser(@Valid @RequestBody UserCreateRequestDto dto) {
        UserSummaryDto response = userManagementService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User created successfully"));
    }

    @GetMapping
    @Operation(summary = "List users", description = "Get paginated users with role, franchiseId, and active status filters")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryDto>>> getAllUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UUID franchiseId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<UserSummaryDto> response = userManagementService.getAllUsers(role, franchiseId, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Users retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update user name, email, role, and franchise assignment")
    public ResponseEntity<ApiResponse<UserSummaryDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequestDto dto) {
        UserSummaryDto response = userManagementService.updateUser(id, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "User updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user status", description = "Activate or deactivate a user (protected against deactivating last active SUPER_ADMIN)")
    public ResponseEntity<ApiResponse<UserSummaryDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequestDto dto) {
        UserSummaryDto response = userManagementService.updateStatus(id, dto.getActive());
        return ResponseEntity.ok(ApiResponse.success(response, "User status updated successfully"));
    }
}
